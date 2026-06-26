package com.vaultpay.api.service;

import com.vaultpay.api.dtos.ContaRequestDTO;
import com.vaultpay.api.dtosResponse.ContaResponse;
import com.vaultpay.api.model.Conta;
import com.vaultpay.api.repository.ContaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContaService {

    private final ContaRepository contaRepository;

    public ContaResponse CriarConta(ContaRequestDTO data){
        if(contaRepository.existsByNumero(data.numero())){
            throw new IllegalArgumentException("Ja existe uma conta registrada com este numero");
        }
        Conta novaConta = Conta.builder().
                numero(data.numero())
                .saldo(data.saldo()).build();

        Conta contaSalvar = contaRepository.save(novaConta);
        return ContaResponse.dto(contaSalvar);
    }
}
