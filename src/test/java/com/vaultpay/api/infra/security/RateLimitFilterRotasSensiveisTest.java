package com.vaultpay.api.infra.security;

import com.vaultpay.api.service.RateLimitingService;
import com.vaultpay.api.service.RegistroRateLimitService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * TDD - Passo 3 do Ciclo dos 4 Passos: estes testes DEVEM FALHAR antes da sua correção.
 *
 * Requisito: escrita financeira (POST /transacao, /transacao/saque, /transacao/deposito)
 * deve usar o balde ESTRITO (RegistroRateLimitService), enquanto leitura (GET) continua
 * no balde folgado (RateLimitingService).
 */
@ExtendWith(MockitoExtension.class)
class RateLimitFilterRotasSensiveisTest {

    @Mock
    private RateLimitingService rateLimitingService;

    @Mock
    private RegistroRateLimitService registroRateLimitService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() {
        Bucket permissivo = mock(Bucket.class);
        lenient().when(permissivo.tryConsume(1)).thenReturn(true);
        lenient().when(rateLimitingService.resolveBucket(anyString())).thenReturn(permissivo);
        lenient().when(registroRateLimitService.resolveBucket(anyString())).thenReturn(permissivo);
        lenient().when(request.getRemoteAddr()).thenReturn("10.10.10.10");
    }

    @ParameterizedTest(name = "POST {0} deve usar o balde ESTRITO")
    @ValueSource(strings = { "/transacao", "/transacao/saque", "/transacao/deposito" })
    @DisplayName("Escritas financeiras devem usar o balde estrito")
    void escritaFinanceiraDeveUsarBaldeEstrito(String uri) throws Exception {
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getMethod()).thenReturn("POST");

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(registroRateLimitService).resolveBucket("10.10.10.10");
        verify(rateLimitingService, never()).resolveBucket(anyString());
    }

    @Test
    @DisplayName("GET de extrato deve continuar no balde folgado")
    void leituraDeExtratoDeveUsarBaldeFolgado() throws Exception {
        when(request.getRequestURI()).thenReturn("/transacao/extrato/1");
        when(request.getMethod()).thenReturn("GET");

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(rateLimitingService).resolveBucket("10.10.10.10");
        verify(registroRateLimitService, never()).resolveBucket(anyString());
    }
}
