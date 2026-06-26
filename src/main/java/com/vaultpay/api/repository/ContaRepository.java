package com.vaultpay.api.repository;

import com.vaultpay.api.model.Conta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContaRepository extends JpaRepository<Conta, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Conta c WHERE c.id = :id")
    Optional<Conta> findByIdWithPessimisticLock(@Param("id") Long id);

    boolean existsByNumero(String numero);
}
