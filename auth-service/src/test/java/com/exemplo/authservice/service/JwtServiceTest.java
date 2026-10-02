package com.exemplo.authservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.exemplo.authservice.model.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtServiceTest {

    private static final String SEGREDO = "00d23b30-8bd5-4cfa-935c-9305be24d5f7";
    private static final long QUINZE_MINUTOS = 900_000L;
    private static final long SETE_DIAS = 604_800_000L;

    private final JwtService jwtService = new JwtService(SEGREDO, QUINZE_MINUTOS, SETE_DIAS);
    private final Usuario usuario = new Usuario("Gustavo Cassel", "gustavo@teste.com", "hash-da-senha");

    private Claims lerClaims(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SEGREDO.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Test
    @DisplayName("accessTokenDeveTrazerEmailNoSubjectETipoAccess")
    void accessTokenDeveTrazerEmailNoSubjectETipoAccess() {
        Claims claims = lerClaims(jwtService.gerarAccessToken(usuario));

        assertThat(claims.getSubject()).isEqualTo("gustavo@teste.com");
        assertThat(claims.get(JwtService.CLAIM_TIPO, String.class)).isEqualTo(JwtService.TIPO_ACCESS);
    }

    @Test
    @DisplayName("todoTokenEmitidoDeveTerDataDeExpiracao")
    void todoTokenEmitidoDeveTerDataDeExpiracao() {
        Claims access = lerClaims(jwtService.gerarAccessToken(usuario));
        Claims refresh = lerClaims(jwtService.gerarRefreshToken(usuario));

        assertThat(access.getExpiration()).isNotNull().isAfter(access.getIssuedAt());
        assertThat(refresh.getExpiration()).isNotNull().isAfter(refresh.getIssuedAt());
        assertThat(refresh.getExpiration()).isAfter(access.getExpiration());
    }

    @Test
    @DisplayName("naoDeveColocarSenhaNoPayloadDoToken")
    void naoDeveColocarSenhaNoPayloadDoToken() {
        Claims claims = lerClaims(jwtService.gerarAccessToken(usuario));

        assertThat(claims.values()).doesNotContain("hash-da-senha");
    }

    @Test
    @DisplayName("deveAceitarRefreshTokenValidoEDevolverEmail")
    void deveAceitarRefreshTokenValidoEDevolverEmail() {
        String refreshToken = jwtService.gerarRefreshToken(usuario);

        assertThat(jwtService.extrairEmailDoRefreshToken(refreshToken)).isEqualTo("gustavo@teste.com");
    }

    @Test
    @DisplayName("deveRecusarAccessTokenNaRotaDeRefresh")
    void deveRecusarAccessTokenNaRotaDeRefresh() {
        String accessToken = jwtService.gerarAccessToken(usuario);

        assertThatThrownBy(() -> jwtService.extrairEmailDoRefreshToken(accessToken))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("nao e um refresh token")
                .extracting(excecao -> ((ResponseStatusException) excecao).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("deveRecusarRefreshTokenExpirado")
    void deveRecusarRefreshTokenExpirado() {
        JwtService emissorComRefreshVencido = new JwtService(SEGREDO, QUINZE_MINUTOS, -1_000L);
        String refreshVencido = emissorComRefreshVencido.gerarRefreshToken(usuario);

        assertThatThrownBy(() -> jwtService.extrairEmailDoRefreshToken(refreshVencido))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("invalido ou expirado");
    }

    @Test
    @DisplayName("deveRecusarTokenAssinadoComOutroSegredo")
    void deveRecusarTokenAssinadoComOutroSegredo() {
        JwtService outroEmissor = new JwtService("9f1c0a52-7c4e-4a1b-9d3f-2b8e5c6a7d40", QUINZE_MINUTOS, SETE_DIAS);
        String tokenFalsificado = outroEmissor.gerarRefreshToken(usuario);

        assertThatThrownBy(() -> jwtService.extrairEmailDoRefreshToken(tokenFalsificado))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("invalido ou expirado");
    }

    @Test
    @DisplayName("deveRecusarRefreshTokenAusenteOuVazio")
    void deveRecusarRefreshTokenAusenteOuVazio() {
        assertThatThrownBy(() -> jwtService.extrairEmailDoRefreshToken(null))
                .isInstanceOf(ResponseStatusException.class);

        assertThatThrownBy(() -> jwtService.extrairEmailDoRefreshToken("   "))
                .isInstanceOf(ResponseStatusException.class);
    }
}
