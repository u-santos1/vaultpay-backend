package com.vaultpay.api.infra.security;

import com.vaultpay.api.service.RateLimitingService;
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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String ip = getClientIp(request);
        Bucket bucket = rateLimitingService. resolveBucket(ip);
        if(bucket.tryConsume(1)){
            filterChain.doFilter(request, response);
        }
        else{
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"erro\": \"Muitos pedidos! O teu IP foi bloqueado temporariamente por medida de seguranca. Tenta novamente em 1 minuto.\"}");
        }
    }
    private String getClientIp(HttpServletRequest request){
        String ipAddres = request.getHeader("X-Forwarded-For");
        if(ipAddres == null || ipAddres.isEmpty() || "unknown".equalsIgnoreCase(ipAddres)){
            ipAddres = request.getRemoteAddr();
        }
        if(ipAddres != null && ipAddres.contains(",")){
            ipAddres = ipAddres.split(",")[0].trim();
        }
        return ipAddres;
    }
}
