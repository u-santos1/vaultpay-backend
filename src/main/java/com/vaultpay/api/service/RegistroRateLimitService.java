package com.vaultpay.api.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RegistroRateLimitService {

    // Cache Caffeine dedicado a rotas críticas (registro/login): expira após 15 min de inatividade
    private final Cache<String, Bucket> cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(Duration.ofMinutes(15))
            .build();

    public Bucket resolveBucket(String ip) {
        return cache.get(ip, this::novoBaldeRegistro);
    }

    private Bucket novoBaldeRegistro(String ip) {
        // Política rigorosa anti-força bruta: máximo 5 tentativas por minuto por IP
        Bandwidth limite = Bandwidth.builder()
                .capacity(5)
                .refillGreedy(5, Duration.ofMinutes(1))
                .build();

        return Bucket.builder().addLimit(limite).build();
    }
}
