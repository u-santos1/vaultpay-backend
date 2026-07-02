package com.vaultpay.api.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.vaultpay.api.model.Usuario;
import jakarta.annotation.PostConstruct;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    @PostConstruct
    public void validarSecret() {
        if (secret == null || secret.isBlank() || secret.length() < 32) {
            throw new IllegalArgumentException(
                    "FATAL: JWT_SECRET ausente ou muito curto. Mínimo de 32 caracteres exigido para segurança criptográfica.");
        }
    }

    public String gerarToken(Usuario usuario){
        try{
var algoritmo = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("API VAULTPAY")
                    .withSubject(usuario.getEmail())
                    .withExpiresAt(dataExpiracao())
                    .sign(algoritmo);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token jwt", exception);
        }
    }

    public String getSubject(String tokenJWT){
        try {
            var algoritmo = Algorithm.HMAC256(secret);
            JWTVerifier verificador = JWT.require(algoritmo)
                    .withIssuer("API VAULTPAY")
                    .build();
            DecodedJWT decodificador = verificador.verify(tokenJWT);
            return decodificador.getSubject();
        } catch (JWTVerificationException e){
            throw new RuntimeException("Token invalido ou expirado", e);
        }
    }

    public Instant getIssuedAt(String tokenJWT){
        try {
            var algoritmo = Algorithm.HMAC256(secret);
            return JWT.require(algoritmo)
                    .withIssuer("API VAULTPAY")
                    .build()
                    .verify(tokenJWT)
                    .getIssuedAtAsInstant();
        } catch (JWTVerificationException e) {
            throw new RuntimeException("Token invalido ou expirado", e);
        }
    }

    private Instant dataExpiracao() {
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
    }
}
