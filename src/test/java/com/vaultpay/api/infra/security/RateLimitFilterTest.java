package com.vaultpay.api.infra.security;

import com.vaultpay.api.service.RateLimitingService;
import com.vaultpay.api.service.RegistroRateLimitService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

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

    @Test
    @DisplayName("Deve ignorar rate limit para rotas do Swagger/OpenAPI")
    void deveIgnorarSwagger() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");
        when(request.getMethod()).thenReturn("GET");

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(rateLimitingService, registroRateLimitService);
    }

    @Test
    @DisplayName("Deve ignorar rate limit para rotas do Actuator healthcheck")
    void deveIgnorarHealthcheckActuator() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/actuator/health");
        when(request.getMethod()).thenReturn("GET");

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(rateLimitingService, registroRateLimitService);
    }

    @Test
    @DisplayName("Deve priorizar request.getRemoteAddr() e ignorar cabeçalho X-Forwarded-For forjado")
    void deveUsarRemoteAddrEIgnorarXForwardedForForjado() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/transacao");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("192.168.1.50");

        Bucket bucket = mock(Bucket.class);
        when(bucket.tryConsume(1)).thenReturn(true);
        when(rateLimitingService.resolveBucket("192.168.1.50")).thenReturn(bucket);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Garante que o bucket foi resolvido com o remoteAddr do socket e não com cabeçalhos arbitrários
        verify(rateLimitingService).resolveBucket("192.168.1.50");
        verify(request, never()).getHeader(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Deve usar RegistroRateLimitService para rota crítica /login")
    void deveUsarRegistroRateLimitParaLogin() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("200.100.50.25");

        Bucket bucket = mock(Bucket.class);
        when(bucket.tryConsume(1)).thenReturn(true);
        when(registroRateLimitService.resolveBucket("200.100.50.25")).thenReturn(bucket);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(registroRateLimitService).resolveBucket("200.100.50.25");
        verifyNoInteractions(rateLimitingService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Deve retornar HTTP 429 quando limite for excedido em rota sensível")
    void deveRetornar429QuandoLimiteExcedidoEmRotaSensivel() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/usuarios");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("200.100.50.25");

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        Bucket bucket = mock(Bucket.class);
        when(bucket.tryConsume(1)).thenReturn(false);
        when(registroRateLimitService.resolveBucket("200.100.50.25")).thenReturn(bucket);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(response).setContentType("application/json;charset=UTF-8");
        verify(filterChain, never()).doFilter(request, response);
        assertTrue(stringWriter.toString().contains("Muitas tentativas de registro ou login"));
    }

    @Test
    @DisplayName("Deve retornar HTTP 429 quando limite geral for excedido")
    void deveRetornar429QuandoLimiteGeralExcedido() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/contas");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("10.0.0.5");

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        Bucket bucket = mock(Bucket.class);
        when(bucket.tryConsume(1)).thenReturn(false);
        when(rateLimitingService.resolveBucket("10.0.0.5")).thenReturn(bucket);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(filterChain, never()).doFilter(request, response);
        assertTrue(stringWriter.toString().contains("Limite de requisições por minuto excedido"));
    }
}
