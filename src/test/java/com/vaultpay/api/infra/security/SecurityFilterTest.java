package com.vaultpay.api.infra.security;

import com.vaultpay.api.PerfilDeAcesso;
import com.vaultpay.api.infra.exception.TokenException;
import com.vaultpay.api.model.Usuario;
import com.vaultpay.api.repository.UsuarioRepository;
import com.vaultpay.api.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityFilterTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private SecurityFilter securityFilter;

    @AfterEach
    void tearDown() {
        // Limpa o contexto de segurança entre os testes para evitar vazamento de estado
        SecurityContextHolder.clearContext();
    }

    // =========================================================
    // Cenário: sem token (requisição pública)
    // =========================================================

    @Test
    @DisplayName("doFilterInternal: sem header Authorization → passa para o próximo filtro sem autenticar")
    void semToken_DevePassarParaProximoFiltroSemAutenticar() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(tokenService);
    }

    @Test
    @DisplayName("doFilterInternal: header sem prefixo 'Bearer ' → passa sem autenticar")
    void headerSemBearerPrefix_DevePassarSemAutenticar() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic usuario:senha");

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(tokenService);
    }

    // =========================================================
    // Cenário: token válido
    // =========================================================

    @Test
    @DisplayName("doFilterInternal: token válido → autentica e continua a cadeia")
    void tokenValido_DeveAutenticarEContinuar() throws Exception {
        String tokenJWT = "token.valido.aqui";
        String email = "user@vaultpay.com";
        Instant issuedAt = Instant.now().minusSeconds(10);

        Usuario usuario = criarUsuario(email, true, null);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + tokenJWT);
        when(tokenService.getSubject(tokenJWT)).thenReturn(email);
        when(tokenService.getIssuedAt(tokenJWT)).thenReturn(issuedAt);
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(usuario, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    @Test
    @DisplayName("doFilterInternal: header 'bearer ' em minúsculas deve funcionar normalmente")
    void headerBearerMinusculo_DeveAutenticar() throws Exception {
        String tokenJWT = "token.valido.aqui";
        String email = "user@vaultpay.com";
        Instant issuedAt = Instant.now().minusSeconds(5);
        Usuario usuario = criarUsuario(email, true, null);

        when(request.getHeader("Authorization")).thenReturn("bearer " + tokenJWT);
        when(tokenService.getSubject(tokenJWT)).thenReturn(email);
        when(tokenService.getIssuedAt(tokenJWT)).thenReturn(issuedAt);
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    // =========================================================
    // Cenário: usuário desativado/bloqueado
    // =========================================================

    @Test
    @DisplayName("doFilterInternal: usuário inativo → retorna 401")
    void usuarioInativo_DeveRetornar401() throws Exception {
        String tokenJWT = "token.valido.aqui";
        String email = "inativo@vaultpay.com";
        Instant issuedAt = Instant.now().minusSeconds(5);

        Usuario usuarioInativo = criarUsuario(email, false, null);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + tokenJWT);
        when(tokenService.getSubject(tokenJWT)).thenReturn(email);
        when(tokenService.getIssuedAt(tokenJWT)).thenReturn(issuedAt);
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuarioInativo));

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    // =========================================================
    // Cenário: token revogado (senha alterada após emissão)
    // =========================================================

    @Test
    @DisplayName("doFilterInternal: token emitido antes da última troca de senha → retorna 401")
    void tokenRevogado_DeveRetornar401() throws Exception {
        String tokenJWT = "token.antigo.aqui";
        String email = "user@vaultpay.com";

        // Token emitido ontem, senha alterada hoje
        Instant issuedAt = Instant.now().minusSeconds(86400);
        Instant dataAlteracaoSenha = Instant.now().minusSeconds(3600);

        Usuario usuario = criarUsuario(email, true, dataAlteracaoSenha);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + tokenJWT);
        when(tokenService.getSubject(tokenJWT)).thenReturn(email);
        when(tokenService.getIssuedAt(tokenJWT)).thenReturn(issuedAt);
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal: token emitido APÓS a última troca de senha → deve autenticar normalmente")
    void tokenEmitidoAposAlteracaoSenha_DeveAutenticar() throws Exception {
        String tokenJWT = "token.novo.aqui";
        String email = "user@vaultpay.com";

        // Senha alterada há 1 hora, token gerado há 30 minutos
        Instant dataAlteracaoSenha = Instant.now().minusSeconds(3600);
        Instant issuedAt = Instant.now().minusSeconds(1800);

        Usuario usuario = criarUsuario(email, true, dataAlteracaoSenha);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + tokenJWT);
        when(tokenService.getSubject(tokenJWT)).thenReturn(email);
        when(tokenService.getIssuedAt(tokenJWT)).thenReturn(issuedAt);
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    // =========================================================
    // Cenário: token inválido/expirado (exceção do TokenService)
    // =========================================================

    @Test
    @DisplayName("doFilterInternal: token com assinatura inválida → retorna 401")
    void tokenInvalido_DeveRetornar401ELimparContexto() throws Exception {
        String tokenInvalido = "header.payload.assinaturaFalsa";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + tokenInvalido);
        when(tokenService.getSubject(tokenInvalido))
                .thenThrow(new RuntimeException("Token invalido ou expirado"));

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("doFilterInternal: usuário não encontrado no banco → retorna 401")
    void usuarioNaoEncontrado_DeveRetornar401() throws Exception {
        String tokenJWT = "token.valido.mas.usuario.removido";
        String email = "deletado@vaultpay.com";
        Instant issuedAt = Instant.now().minusSeconds(5);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + tokenJWT);
        when(tokenService.getSubject(tokenJWT)).thenReturn(email);
        when(tokenService.getIssuedAt(tokenJWT)).thenReturn(issuedAt);
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.empty());

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
    }

    // =========================================================
    // Helpers
    // =========================================================

    private Usuario criarUsuario(String email, boolean ativo, Instant dataUltimaAlteracaoSenha) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail(email);
        usuario.setSenha("senha_hash");
        usuario.setAtivo(ativo);
        usuario.setPerfilDeAcesso(PerfilDeAcesso.USER);
        usuario.setDataUltimaAlteracaoSenha(dataUltimaAlteracaoSenha);
        return usuario;
    }
}
