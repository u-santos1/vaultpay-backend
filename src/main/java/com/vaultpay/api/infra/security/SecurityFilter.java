package com.vaultpay.api.infra.security;

import com.vaultpay.api.infra.exception.TokenException;
import com.vaultpay.api.repository.UsuarioRepository;
import com.vaultpay.api.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.http.SecurityHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityFilter extends OncePerRequestFilter {
    private final TokenService tokenService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException{
        try {
            var tokenJWT = recuperarToken(request);
            if (tokenJWT != null){
                var subject = tokenService.getSubject(tokenJWT);
                var issuedAt = tokenService.getIssuedAt(tokenJWT);

                var usuario = usuarioRepository.findByEmail(subject)
                        .orElseThrow(()-> new UsernameNotFoundException("Usuario nao encontrado"));
                if (!usuario.isEnabled() || !usuario.isAccountNonLocked()){
                    throw new TokenException("Usuario desativado ou bloquado");
                }
                if (usuario.getDataUltimaAlteracaoSenha() != null && issuedAt.isBefore(usuario.getDataUltimaAlteracaoSenha())){
                    throw new TokenException("Token revogado: a senha foi alterado recentemente.");
                }
                var authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }catch (Exception e){
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token invalido ou expirado");
            return;
        }filterChain.doFilter(request, response);

    }
    private String recuperarToken(HttpServletRequest request){
        var authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.toLowerCase().startsWith("bearer ")){
            return authorizationHeader.substring(7).trim();
        }return null;

    }
}
