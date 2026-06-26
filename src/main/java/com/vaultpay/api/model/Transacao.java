package com.vaultpay.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transacoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "conta_origem_id", nullable = false)
    private Conta contaOrigem;

    @ManyToOne(optional = false)
    @JoinColumn(name = "conta_destino_id", nullable = false)
    private Conta contaDestino;

    @Column(nullable = false)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDateTime dataHora;
}
