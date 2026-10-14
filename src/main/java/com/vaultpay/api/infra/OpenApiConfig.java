package com.vaultpay.api.infra;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracao do Swagger: habilita o botao "Authorize" para colar o token JWT
 * obtido em POST /login.
 */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "VaultPay API",
        version = "1.0",
        description = "API de carteira digital: contas, depositos, saques, transferencias e extrato."))
@SecurityScheme(
        name = "bearer-key",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {
}
