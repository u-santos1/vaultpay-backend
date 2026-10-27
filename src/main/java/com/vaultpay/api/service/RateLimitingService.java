package com.vaultpay.api.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;


import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Service
public class RateLimitingService {

    private final Cache<String, Bucket> cache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterAccess(Duration.ofMinutes(10))
        .build();

        public Bucket resolveBucket(String ip) {
            return cache.get(ip, this::novoBalde);
        }

        private Bucket novoBalde(String ip) {
            Bandwidth limite = Bandwidth.builder()
            .capacity(5)
            .refillGreedy(5, Duration.ofMinutes(1))
            .build();

            return Bucket.builder().addLimit(limite).build();
        }
}
