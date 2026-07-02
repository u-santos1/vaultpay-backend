package com.vaultpay.api.service;

import com.vaultpay.api.dtos.ContaRequestDTO;
import com.vaultpay.api.dtosResponse.ContaResponse;
import com.vaultpay.api.model.Conta;
import com.vaultpay.api.model.Usuario;
import com.vaultpay.api.repository.ContaRepository;
import com.vaultpay.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ContaService {

    private final ContaRepository contaRepository;
    private final UsuarioRepository usuarioRepository;

    public ContaResponse CriarConta(ContaRequestDTO data){
        if(contaRepository.existsByNumero(data.numero())){
            throw new IllegalArgumentException("Ja existe uma conta registrada com este numero");
        }
        Usuario usuario = usuarioRepository.findById(data.usuarioId())
                .orElseThrow(()-> new IllegalArgumentException("Não é possível criar a conta: Usuário não encontrado."));
        Conta novaConta = Conta.builder().
                numero(data.numero())
                .saldo(data.saldo())
                .usuario(usuario)
                .limiteTransacao(new BigDecimal("10000.00"))
                .ativo(true)
                .build();

        Conta contaSalvar = contaRepository.save(novaConta);
        return ContaResponse.dto(contaSalvar);
    }
}
