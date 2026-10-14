package com.vaultpay.api.dtosResponse;

import com.vaultpay.api.PerfilDeAcesso;
import com.vaultpay.api.model.Conta;
import com.vaultpay.api.model.Usuario;

public record UsuarioResponseDTO(
        Long id,
        String email,
        PerfilDeAcesso perfil,
        Long contaId,
        String numeroConta
) {
    public static UsuarioResponseDTO fromEntity(Usuario usuario, Conta conta) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getPerfilDeAcesso(),
                conta.getId(),
                conta.getNumero()
        );
    }
}
