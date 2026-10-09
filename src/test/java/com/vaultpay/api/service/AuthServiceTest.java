package com.vaultpay.api.service;

import com.vaultpay.api.PerfilDeAcesso;
import com.vaultpay.api.model.Usuario;
import com.vaultpay.api.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AuthService authService;

    // =========================================================
    // loadUserByUsername
    // =========================================================

    @Test
    @DisplayName("loadUserByUsername: deve retornar o usuário quando email existir")
    void loadUserByUSername_DeveRetornarUsuarioQuandoEmailExistir() {

        String email = "user@vaultpay.com";
        Usuario usuarioEsperado = criarUsuario(email);
        when(usuarioRepository.findByEmail(email))
                .thenReturn(Optional.of(usuarioEsperado));

        UserDetails resultado = authService.loadUserByUsername(email);

        assertNotNull(resultado);
        assertEquals(email, resultado.getUsername()); // compara String com String
        verify(usuarioRepository, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("loadUserByUsername: deve lançar UsernameNotFoundException quando email não existir")
    void loadUserByUsername_DeveLancarExcecaoQuandoEmailNaoExistir() {
        String email = "inexistente@vaultpay.com";

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> authService.loadUserByUsername(email));

        assertEquals("Credenciais invalidas", exception.getMessage());
        verify(usuarioRepository, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("loadUserByUsername: usuário USER deve ter apenas ROLE_USER")
    void loadUserByUsername_UsuarioComumDeveTerApenasRoleUser() {
        Usuario usuario = criarUsuario("user@vaultpay.com");
        usuario.setPerfilDeAcesso(PerfilDeAcesso.USER);

        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));

        UserDetails resultado = authService.loadUserByUsername(usuario.getEmail());

        assertTrue(resultado.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        assertFalse(resultado.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("loadUserByUsername: usuário ADMIN deve ter ROLE_ADMIN e ROLE_USER")
    void loadUserByUsername_AdminDeveTerRoleAdminERoleUser() {
        Usuario admin = criarUsuario("admin@vaultpay.com");
        admin.setPerfilDeAcesso(PerfilDeAcesso.ADMIN);

        when(usuarioRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));

        UserDetails resultado = authService.loadUserByUsername(admin.getEmail());

        assertTrue(resultado.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(resultado.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    @DisplayName("loadUserByUsername: usuário inativo deve retornar isEnabled=false")
    void loadUserByUsername_UsuarioInativoDeveRetornarEnabledFalse() {
        Usuario usuario = criarUsuario("inativo@vaultpay.com");
        usuario.setAtivo(false);

        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));

        UserDetails resultado = authService.loadUserByUsername(usuario.getEmail());

        assertFalse(resultado.isEnabled());
        assertFalse(resultado.isAccountNonLocked());
    }

    

    // =========================================================
    // Helpers
    // =========================================================

    private Usuario criarUsuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail(email);
        usuario.setSenha("senha_hash");
        usuario.setAtivo(true);
        usuario.setPerfilDeAcesso(PerfilDeAcesso.USER);
        return usuario;
    }
}
