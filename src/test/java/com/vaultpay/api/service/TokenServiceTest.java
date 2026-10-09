package com.vaultpay.api.service;

import com.vaultpay.api.PerfilDeAcesso;
import com.vaultpay.api.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {

    private TokenService tokenService;

    // Secret mínimo de 32 chars para passar na validação de @PostConstruct
    private static final String SECRET_VALIDO = "minha-chave-super-secreta-para-testes-junit";

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", SECRET_VALIDO);
        tokenService.validarSecret(); // simula o @PostConstruct
    }

    // =========================================================
    // gerarToken
    // =========================================================

    @Test
    @DisplayName("gerarToken: deve retornar um token não nulo e não vazio")
    void gerarToken_DeveRetornarTokenValido() {
        Usuario usuario = criarUsuario("user@vaultpay.com");

        String token = tokenService.gerarToken(usuario);

        assertNotNull(token);
        assertFalse(token.isBlank());
        // JWT tem 3 partes separadas por ponto
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    @DisplayName("gerarToken: usuários diferentes devem gerar tokens diferentes")
    void gerarToken_UsuariosDiferentesDevemGerarTokensDiferentes() {
        Usuario usuario1 = criarUsuario("a@vaultpay.com");
        Usuario usuario2 = criarUsuario("b@vaultpay.com");

        String token1 = tokenService.gerarToken(usuario1);
        String token2 = tokenService.gerarToken(usuario2);

        assertNotEquals(token1, token2);
    }

    // =========================================================
    // getSubject
    // =========================================================

    @Test
    @DisplayName("getSubject: deve extrair o email do subject do token")
    void getSubject_DeveRetornarEmailCorreto() {
        String email = "joao@vaultpay.com";
        Usuario usuario = criarUsuario(email);

        String token = tokenService.gerarToken(usuario);
        String subject = tokenService.getSubject(token);

        assertEquals(email, subject);
    }

    @Test
    @DisplayName("getSubject: deve lançar RuntimeException para token inválido")
    void getSubject_DeveLancarExcecaoParaTokenInvalido() {
        String tokenInvalido = "header.payload.assinatura_falsa";

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tokenService.getSubject(tokenInvalido));

        assertTrue(exception.getMessage().contains("Token invalido ou expirado"));
    }

    @Test
    @DisplayName("getSubject: deve lançar RuntimeException para token com secret errado")
    void getSubject_DeveLancarExcecaoParaSecretErrado() {
        // Gera token com um service usando secret diferente
        TokenService outroService = new TokenService();
        ReflectionTestUtils.setField(outroService, "secret", "outro-secret-completamente-diferente-4321");
        outroService.validarSecret();

        String tokenDeOutroServico = outroService.gerarToken(criarUsuario("user@vaultpay.com"));

        // Tenta verificar com o service original (secret diferente) → deve falhar
        assertThrows(RuntimeException.class, () -> tokenService.getSubject(tokenDeOutroServico));
    }

    // =========================================================
    // getIssuedAt
    // =========================================================

    @Test
    @DisplayName("getIssuedAt: deve retornar um Instant próximo do momento atual")
    void getIssuedAt_DeveRetornarInstantDeEmissaoCorreto() {
        Usuario usuario = criarUsuario("ana@vaultpay.com");
        Instant antes = Instant.now().minusSeconds(2);

        String token = tokenService.gerarToken(usuario);
        Instant issuedAt = tokenService.getIssuedAt(token);

        Instant depois = Instant.now().plusSeconds(2);

        assertNotNull(issuedAt);
        assertTrue(issuedAt.isAfter(antes), "issuedAt deve ser após o momento antes da geração");
        assertTrue(issuedAt.isBefore(depois), "issuedAt deve ser antes do momento após a geração");
    }

    @Test
    @DisplayName("getIssuedAt: deve lançar RuntimeException para token inválido")
    void getIssuedAt_DeveLancarExcecaoParaTokenInvalido() {
        assertThrows(RuntimeException.class,
                () -> tokenService.getIssuedAt("token.invalido.aqui"));
    }

    // =========================================================
    // validarSecret (@PostConstruct)
    // =========================================================

    @Test
    @DisplayName("validarSecret: deve lançar IllegalArgumentException para secret nulo")
    void validarSecret_DeveLancarExcecaoParaSecretNulo() {
        TokenService serviceComSecretNulo = new TokenService();
        ReflectionTestUtils.setField(serviceComSecretNulo, "secret", null);

        assertThrows(IllegalArgumentException.class, serviceComSecretNulo::validarSecret);
    }

    @Test
    @DisplayName("validarSecret: deve lançar IllegalArgumentException para secret menor que 32 chars")
    void validarSecret_DeveLancarExcecaoParaSecretCurto() {
        TokenService serviceComSecretCurto = new TokenService();
        ReflectionTestUtils.setField(serviceComSecretCurto, "secret", "curto");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, serviceComSecretCurto::validarSecret);

        assertTrue(exception.getMessage().contains("Mínimo de 32 caracteres"));
    }

    @Test
    @DisplayName("validarSecret: deve lançar IllegalArgumentException para secret em branco")
    void validarSecret_DeveLancarExcecaoParaSecretEmBranco() {
        TokenService serviceComSecretBranco = new TokenService();
        ReflectionTestUtils.setField(serviceComSecretBranco, "secret", "   ");

        assertThrows(IllegalArgumentException.class, serviceComSecretBranco::validarSecret);
    }

    // =========================================================
    // Helpers
    // =========================================================

    private Usuario criarUsuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail(email);
        usuario.setSenha("senha_hash");
        usuario.setAtivo(true);
        usuario.setPerfilDeAcesso(PerfilDeAcesso.USER);
        return usuario;
    }
}
