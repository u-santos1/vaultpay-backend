package com.vaultpay.api.model;

import com.vaultpay.api.StatusTransacao;
import com.vaultpay.api.TipoTransacao;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoTransacao tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusTransacao status;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Column(unique = true, nullable = false)
    private String chaveIdempotencia;


}
