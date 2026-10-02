package com.exemplo.authservice.controller;

import com.exemplo.authservice.dto.LoginRequest;
import com.exemplo.authservice.dto.LoginResponse;
import com.exemplo.authservice.dto.RefreshRequest;
import com.exemplo.authservice.dto.UsuarioRequest;
import com.exemplo.authservice.model.Usuario;
import com.exemplo.authservice.service.JwtService;
import com.exemplo.authservice.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private static final String TIPO_BEARER = "Bearer";

    private final UsuarioService service;
    private final JwtService jwtService;

    @PostMapping
    public ResponseEntity<Long> cadastrar(@Valid @RequestBody UsuarioRequest request) {
        Usuario usuario = service.cadastrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuario.getId());
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        Usuario usuario = this.service.autenticar(request);
        return ResponseEntity.ok(montarResposta(usuario));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody RefreshRequest request) {
        String email = jwtService.extrairEmailDoRefreshToken(request.getRefreshToken());
        Usuario usuario = this.service.buscarPorEmail(email);
        return ResponseEntity.ok(montarResposta(usuario));
    }

    private LoginResponse montarResposta(Usuario usuario) {
        return new LoginResponse(
                jwtService.gerarAccessToken(usuario),
                jwtService.gerarRefreshToken(usuario),
                TIPO_BEARER,
                jwtService.getExpiracaoAccessSegundos());
    }
}
