package com.vaultpay.api.service;

import com.vaultpay.api.PerfilDeAcesso;
import com.vaultpay.api.dtos.CadastroUsuarioRequestDTO;
import com.vaultpay.api.dtosResponse.UsuarioResponseDTO;
import com.vaultpay.api.model.Conta;
import com.vaultpay.api.model.Usuario;
import com.vaultpay.api.repository.ContaRepository;
import com.vaultpay.api.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ContaRepository contaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void cadastrar_DeveCriarUsuarioEContaComSucesso() {
        CadastroUsuarioRequestDTO request = new CadastroUsuarioRequestDTO("novo@vaultpay.com", "senha123");

        when(usuarioRepository.existsByEmail("novo@vaultpay.com")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("$2a$10$encodedHash123");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(contaRepository.existsByNumero(anyString())).thenReturn(false);
        when(contaRepository.save(any(Conta.class))).thenAnswer(invocation -> {
            Conta c = invocation.getArgument(0);
            c.setId(10L);
            return c;
        });

        UsuarioResponseDTO response = usuarioService.cadastrar(request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("novo@vaultpay.com", response.email());
        assertEquals(PerfilDeAcesso.USER, response.perfil());
        assertEquals(10L, response.contaId());
        assertNotNull(response.numeroConta());

        verify(passwordEncoder).encode("senha123");
        verify(usuarioRepository).save(argThat(u ->
                u.getEmail().equals("novo@vaultpay.com") &&
                u.getSenha().equals("$2a$10$encodedHash123") &&
                u.isAtivo() &&
                u.getPerfilDeAcesso() == PerfilDeAcesso.USER
        ));
        verify(contaRepository).save(argThat(c ->
                c.getSaldo().compareTo(BigDecimal.ZERO) == 0 &&
                c.getAtivo() &&
                c.getUsuario().getId().equals(1L)
        ));
    }

    @Test
    void cadastrar_DeveLancarExcecaoQuandoEmailJaExistir() {
        CadastroUsuarioRequestDTO request = new CadastroUsuarioRequestDTO("existente@vaultpay.com", "senha123");

        when(usuarioRepository.existsByEmail("existente@vaultpay.com")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                usuarioService.cadastrar(request)
        );

        assertTrue(exception.getMessage().contains("Ja existe um usuario cadastrado com este e-mail"));
        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(contaRepository, never()).save(any(Conta.class));
    }
}
