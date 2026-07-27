package com.vaultpay.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest
class ApiApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void verHashSenha() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String senhaPlana = "minha_senha_123";

        // Gera o hash utilizando BCrypt (padrão seguro do Spring Security)
        String hashGerado = encoder.encode(senhaPlana);

        System.out.println("========================================");
        System.out.println(" Senha em Texto Plano : " + senhaPlana);
        System.out.println(" Hash BCrypt Gerado   : " + hashGerado);
        System.out.println("========================================");

        // Validação opcional para conferir se o hash bate com a senha
        boolean confere = encoder.matches(senhaPlana, hashGerado);
        System.out.println(" A senha confere com o hash? " + confere);
        System.out.println("========================================");
    }

}
