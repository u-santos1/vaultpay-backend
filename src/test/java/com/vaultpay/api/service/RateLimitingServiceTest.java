package com.vaultpay.api.service;

import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD - Passo 3 do Ciclo dos 4 Passos: este teste DEVE FALHAR antes da sua correção.
 *
 * Cenário: um usuário legítimo navegando no extrato (GET) com paginação faz
 * dezenas de requisições em um minuto. O balde "geral" não pode ser tão
 * apertado quanto o balde de /login e /usuarios (5 req/min).
 */
class RateLimitingServiceTest {

    private static final int LIMITE_LEITURA_POR_MINUTO = 60;

    private final RateLimitingService service = new RateLimitingService();

    @Test
    @DisplayName("Balde geral (leitura) deve permitir 60 requisições/minuto por IP")
    void deveAceitar60RequisicoesDeLeituraPorMinuto() {
        Bucket bucket = service.resolveBucket("10.0.0.1");

        for (int i = 1; i <= LIMITE_LEITURA_POR_MINUTO; i++) {
            assertTrue(bucket.tryConsume(1),
                    "Requisição de leitura #" + i + " deveria ser aceita (limite = 60/min)");
        }
    }

    @Test
    @DisplayName("Balde geral deve bloquear a 61ª requisição no mesmo minuto")
    void deveBloquearAposEstourarLimiteDeLeitura() {
        Bucket bucket = service.resolveBucket("10.0.0.2");

        for (int i = 0; i < LIMITE_LEITURA_POR_MINUTO; i++) {
            bucket.tryConsume(1);
        }

        assertFalse(bucket.tryConsume(1),
                "A 61ª requisição no mesmo minuto deve ser bloqueada");
    }

    @Test
    @DisplayName("Mesmo IP deve reutilizar o mesmo balde; IPs diferentes têm baldes independentes")
    void devePreservarIsolamentoPorIp() {
        Bucket a1 = service.resolveBucket("10.0.0.3");
        Bucket a2 = service.resolveBucket("10.0.0.3");
        Bucket b = service.resolveBucket("10.0.0.4");

        assertSame(a1, a2);
        assertTrue(b.tryConsume(1));
    }
}
