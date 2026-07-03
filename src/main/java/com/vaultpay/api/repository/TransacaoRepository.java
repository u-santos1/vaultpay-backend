package com.vaultpay.api.repository;

import com.vaultpay.api.model.Transacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, UUID> {
    boolean existsByChaveIdempotencia(String chave);


    Page<Transacao> findByContaOrigemIdOrContaDestinoId(
            Long contaOrigemId, Long contaDestinoId, Pageable pageable);


}
