package com.vaultpay.api.ai;

import com.vaultpay.api.model.Conta;
import com.vaultpay.api.model.Usuario;
import com.vaultpay.api.repository.ContaRepository;
import com.vaultpay.api.repository.TransacaoRepository;
import com.vaultpay.api.repository.UsuarioRepository;
import com.vaultpay.api.service.TransacaoService;
import com.vaultpay.api.dtos.TransacaoResponseDTO;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TDD - Passo 3 do Ciclo dos 4 Passos: este teste DEVE FALHAR antes da sua correção.
 *
 * Objetivo de Segurança (Anti-IDOR via LLM):
 * Nenhuma Tool de IA deve aceitar 'usuarioId' como parâmetro preenchido pelo modelo.
 * A identidade do usuário autenticado deve vir estritamente do SecurityContextHolder.
 */
@ExtendWith(MockitoExtension.class)
class VaultPayToolsSecurityTest {

    @Mock
    private ContaRepository contaRepository;

    @Mock
    private TransacaoRepository transacaoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TransacaoService transacaoService;

    @InjectMocks
    private VaultPayTools tools;

    private Usuario usuarioAutenticado;
    private Conta contaUsuarioAutenticado;

    @BeforeEach
    void setUp() {
        usuarioAutenticado = new Usuario();
        usuarioAutenticado.setId(10L);
        usuarioAutenticado.setEmail("alice@vaultpay.com");

        contaUsuarioAutenticado = Conta.builder()
                .id(100L)
                .numero("12345-6")
                .saldo(new BigDecimal("1500.00"))
                .limiteTransacao(new BigDecimal("5000.00"))
                .ativo(true)
                .usuario(usuarioAutenticado)
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(usuarioAutenticado, null, usuarioAutenticado.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Nenhuma tool de IA deve aceitar usuarioId como parâmetro do LLM (Anti-Prompt Injection / Anti-IDOR)")
    void toolsNaoDevemAceitarUsuarioIdComoParametro() {
        Method[] methods = VaultPayTools.class.getDeclaredMethods();

        for (Method method : methods) {
            if (method.isAnnotationPresent(Tool.class)) {
                for (Parameter param : method.getParameters()) {
                    assertFalse(param.getName().equalsIgnoreCase("usuarioId"),
                            "VULNERABILIDADE IDOR: A tool '" + method.getName()
                                    + "' aceita o parâmetro '" + param.getName()
                                    + "' vindo do LLM! O LLM não pode escolher quem é o usuário.");

                    if (param.isAnnotationPresent(P.class)) {
                        P p = param.getAnnotation(P.class);
                        assertFalse(p.value().toLowerCase().contains("usuário autenticado")
                                        || p.value().toLowerCase().contains("id do usuário"),
                                "VULNERABILIDADE IDOR: A anotação @P da tool '" + method.getName()
                                        + "' instrui o LLM a enviar identificador de usuário: " + p.value());
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("consultarSaldo deve buscar a conta do usuário no SecurityContextHolder sem receber parâmetros do LLM")
    void consultarSaldoDeveUsarUsuarioAutenticadoDoSecurityContext() throws Exception {
        Method method = VaultPayTools.class.getMethod("consultarSaldo");
        assertEquals(0, method.getParameterCount(),
                "A tool consultarSaldo() deve ter 0 parâmetros; a identidade vem do SecurityContextHolder.");

        when(contaRepository.findByUsuarioId(10L)).thenReturn(Optional.of(contaUsuarioAutenticado));

        String resultado = (String) method.invoke(tools);

        assertNotNull(resultado);
        assertTrue(resultado.contains("1.500,00"), "Deveria retornar o saldo formatado do usuário autenticado (Alice)");
        verify(contaRepository, times(1)).findByUsuarioId(10L);
        verify(contaRepository, never()).findByUsuarioId(argThat(id -> !id.equals(10L)));
    }

    @Test
    @DisplayName("consultarExtrato deve buscar movimentações do usuário do SecurityContextHolder sem receber usuarioId")
    void consultarExtratoDeveUsarUsuarioAutenticadoDoSecurityContext() throws Exception {
        Method method = VaultPayTools.class.getMethod("consultarExtrato");
        assertEquals(0, method.getParameterCount(),
                "A tool consultarExtrato() deve ter 0 parâmetros; a identidade vem do SecurityContextHolder.");

        when(contaRepository.findByUsuarioId(10L)).thenReturn(Optional.of(contaUsuarioAutenticado));
        when(transacaoRepository.findByContaOrigemIdOrContaDestinoId(any(), any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        method.invoke(tools);

        verify(contaRepository, times(1)).findByUsuarioId(10L);
    }

    @Test
    @DisplayName("transferir deve debitar da conta do usuário no SecurityContextHolder sem aceitar usuarioId do LLM")
    void transferirDeveUsarContaOrigemDoUsuarioAutenticado() throws Exception {
        Method method = VaultPayTools.class.getMethod("transferir", Long.class, Double.class, String.class);
        assertEquals(3, method.getParameterCount(),
                "A tool transferir() deve ter exatamente 3 parâmetros (destino, valor, descricao); a origem vem do SecurityContextHolder.");

        when(contaRepository.findByUsuarioId(10L)).thenReturn(Optional.of(contaUsuarioAutenticado));

        TransacaoResponseDTO mockResponse = new TransacaoResponseDTO(
                java.util.UUID.randomUUID(),
                100L,
                200L,
                new BigDecimal("50.00"),
                java.time.LocalDateTime.now()
        );
        when(transacaoService.realizarTransferencia(any(), any(), eq(usuarioAutenticado)))
                .thenReturn(mockResponse);

        String resultado = (String) method.invoke(tools, 200L, 50.0, "PIX");

        assertNotNull(resultado);
        assertTrue(resultado.contains("Transferência realizada com sucesso!"));
        verify(contaRepository, times(1)).findByUsuarioId(10L);
        // Garante que o transacaoService foi chamado com o usuário autenticado da sessão (Alice), e NUNCA outro usuário
        verify(transacaoService).realizarTransferencia(
                argThat(dto -> dto.idContaOrigem().equals(100L) && dto.idContaDestino().equals(200L)),
                anyString(),
                eq(usuarioAutenticado)
        );
    }
}
