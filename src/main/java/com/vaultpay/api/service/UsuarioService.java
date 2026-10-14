package com.vaultpay.api.service;

import com.vaultpay.api.PerfilDeAcesso;
import com.vaultpay.api.dtos.CadastroUsuarioRequestDTO;
import com.vaultpay.api.dtosResponse.UsuarioResponseDTO;
import com.vaultpay.api.model.Conta;
import com.vaultpay.api.model.Usuario;
import com.vaultpay.api.repository.ContaRepository;
import com.vaultpay.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ContaRepository contaRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioResponseDTO cadastrar(CadastroUsuarioRequestDTO data) {
        if (usuarioRepository.existsByEmail(data.email())) {
            throw new IllegalArgumentException("Ja existe um usuario cadastrado com este e-mail.");
        }

        Usuario novoUsuario = new Usuario();
        novoUsuario.setEmail(data.email());
        novoUsuario.setSenha(passwordEncoder.encode(data.senha()));
        novoUsuario.setAtivo(true);
        novoUsuario.setPerfilDeAcesso(PerfilDeAcesso.USER);
        novoUsuario.setDataUltimaAlteracaoSenha(Instant.now());

        Usuario usuarioSalvo = usuarioRepository.save(novoUsuario);

        Conta novaConta = Conta.builder()
                .numero(gerarNumeroContaUnico())
                .saldo(BigDecimal.ZERO)
                .limiteTransacao(new BigDecimal("10000.00"))
                .ativo(true)
                .usuario(usuarioSalvo)
                .build();

        Conta contaSalva = contaRepository.save(novaConta);

        return UsuarioResponseDTO.fromEntity(usuarioSalvo, contaSalva);
    }

    private String gerarNumeroContaUnico() {
        String numero;
        do {
            int agencia = ThreadLocalRandom.current().nextInt(10000, 99999);
            int digito = ThreadLocalRandom.current().nextInt(0, 10);
            numero = String.format("%05d-%d", agencia, digito);
        } while (contaRepository.existsByNumero(numero));
        return numero;
    }
}
