package com.vaultpay.api.service;

import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith (MockitoExtension.class)
public class RelatorioTransacoesTest {

    @Mock 
    private RelatorioTransacoes relatorioTransacoes;

    

    @BeforeEach 
    void setUp() {
        relatorioTransacoes = new RelatorioTransacoes();
    }

    @Test 
    void somarTotalTransacionado_DeveRetornarZeroParaListaVazia() {

        
    }
}
