package com.exemplo.authservice.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.exemplo.authservice.model.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    // os dois tokens usam a mesma chave, entao e o tipo que separa um do outro
    public static final String CLAIM_TIPO = "tipo";
    public static final String TIPO_ACCESS = "access";
    public static final String TIPO_REFRESH = "refresh";

    private final SecretKey chave;
    private final long expiracaoAccessMs;
    private final long expiracaoRefreshMs;

    public JwtService(@Value("${jwt.secret}") String segredo,
            @Value("${jwt.access-token.expiracao-ms}") long expiracaoAccessMs,
            @Value("${jwt.refresh-token.expiracao-ms}") long expiracaoRefreshMs) {
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.expiracaoAccessMs = expiracaoAccessMs;
        this.expiracaoRefreshMs = expiracaoRefreshMs;
    }

    public String gerarAccessToken(Usuario usuario) {
        return gerar(usuario, TIPO_ACCESS, expiracaoAccessMs);
    }

    public String gerarRefreshToken(Usuario usuario) {
        return gerar(usuario, TIPO_REFRESH, expiracaoRefreshMs);
    }

    public long getExpiracaoAccessSegundos() {
        return expiracaoAccessMs / 1000;
    }

    public String extrairEmailDoRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token nao informado");
        }

        Claims claims = lerClaims(refreshToken);

        if (!TIPO_REFRESH.equals(claims.get(CLAIM_TIPO, String.class))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "O token informado nao e um refresh token");
        }

        return claims.getSubject();
    }

    private String gerar(Usuario usuario, String tipo, long duracaoMs) {
        Instant agora = Instant.now();

        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("nome", usuario.getNome())
                .claim(CLAIM_TIPO, tipo)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusMillis(duracaoMs)))
                .signWith(chave)
                .compact();
    }

    private Claims lerClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(chave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Refresh token invalido ou expirado");
        }
    }
}
