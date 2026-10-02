package com.exemplo.authservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.exemplo.authservice.model.Usuario;
import com.exemplo.authservice.service.JwtService;
import com.exemplo.authservice.service.UsuarioService;

@WebMvcTest(UsuarioController.class)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioService service;

    @MockBean
    private JwtService jwtService;

    private final Usuario usuario = new Usuario("Gustavo Cassel", "gustavo@teste.com", "hash-da-senha");

    @BeforeEach
    void configurarEmissor() {
        when(jwtService.gerarAccessToken(any(Usuario.class))).thenReturn("token-de-acesso");
        when(jwtService.gerarRefreshToken(any(Usuario.class))).thenReturn("token-de-renovacao");
        when(jwtService.getExpiracaoAccessSegundos()).thenReturn(900L);
    }

    @Test
    @DisplayName("deveCadastrarUsuarioERetornar201")
    void deveCadastrarUsuarioERetornar201() throws Exception {
        Usuario salvo = new Usuario("Gustavo Cassel", "gustavo@teste.com", "hash-da-senha");
        salvo.setId(1L);
        when(service.cadastrar(any())).thenReturn(salvo);

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Gustavo Cassel","email":"gustavo@teste.com","senha":"senha-forte"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("deveRecusarCadastroComDadosInvalidos")
    void deveRecusarCadastroComDadosInvalidos() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"","email":"nao-e-email","senha":"1"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("loginComCredenciaisValidasDeveDevolverOsDoisTokens")
    void loginComCredenciaisValidasDeveDevolverOsDoisTokens() throws Exception {
        when(service.autenticar(any())).thenReturn(usuario);

        mockMvc.perform(post("/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"gustavo@teste.com","senha":"senha-forte"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token-de-acesso"))
                .andExpect(jsonPath("$.refreshToken").value("token-de-renovacao"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiraEmSegundos").value(900));
    }

    @Test
    @DisplayName("loginComCredenciaisInvalidasDeveRetornar401")
    void loginComCredenciaisInvalidasDeveRetornar401() throws Exception {
        when(service.autenticar(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos"));

        mockMvc.perform(post("/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"gustavo@teste.com","senha":"errada"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("refreshComTokenValidoDeveEmitirNovoAccessToken")
    void refreshComTokenValidoDeveEmitirNovoAccessToken() throws Exception {
        when(jwtService.extrairEmailDoRefreshToken("token-de-renovacao")).thenReturn("gustavo@teste.com");
        when(service.buscarPorEmail("gustavo@teste.com")).thenReturn(usuario);

        mockMvc.perform(post("/usuarios/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"token-de-renovacao"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token-de-acesso"))
                .andExpect(jsonPath("$.refreshToken").value("token-de-renovacao"));
    }

    @Test
    @DisplayName("refreshComTokenInvalidoDeveRetornar401")
    void refreshComTokenInvalidoDeveRetornar401() throws Exception {
        when(jwtService.extrairEmailDoRefreshToken(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token invalido ou expirado"));

        mockMvc.perform(post("/usuarios/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"qualquer-coisa"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
