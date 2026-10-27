package com.vaultpay.api.infra.security;

import com.vaultpay.api.service.RateLimitingService;
import com.vaultpay.api.service.RegistroRateLimitService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;
    private final RegistroRateLimitService registroRateLimitService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        // Ignora endpoints de documentação Swagger/OpenAPI
        if (uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = getClientIp(request);

        // Identifica rotas críticas de registro e autenticação (alvos comuns de força bruta e DoS)
        boolean rotaSensivel = (uri.equals("/usuarios") && "POST".equalsIgnoreCase(method))
                || (uri.equals("/login") && "POST".equalsIgnoreCase(method));

        Bucket bucket = rotaSensivel
                ? registroRateLimitService.resolveBucket(ip)
                : rateLimitingService.resolveBucket(ip);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            String mensagem = rotaSensivel
                    ? "{\"erro\": \"Muitas tentativas de registro ou login! Seu IP foi limitado temporariamente por segurança. Tente novamente em 1 minuto.\"}"
                    : "{\"erro\": \"Muitos pedidos! Limite de requisições por minuto excedido. Tente novamente em instantes.\"}";
            response.getWriter().write(mensagem);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        return ipAddress;
    }
}
