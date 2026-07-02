CREATE TABLE tb_usuarios (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    ativo BOOLEAN NOT NULL,
    data_ultima_alteracao_senha TIMESTAMP,
    perfil_de_acesso VARCHAR(255)
);

CREATE TABLE contas (
    id BIGSERIAL PRIMARY KEY,
    numero VARCHAR(255) NOT NULL UNIQUE,
    saldo NUMERIC(38,2) NOT NULL,
    ativo BOOLEAN NOT NULL,
    usuario_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_contas_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuarios(id)
);

CREATE TABLE transacoes (
    id UUID PRIMARY KEY,
    conta_origem_id BIGINT NOT NULL,
    conta_destino_id BIGINT NOT NULL,
    valor NUMERIC(38,2) NOT NULL,
    data_hora TIMESTAMP NOT NULL,
    CONSTRAINT fk_transacao_conta_origem FOREIGN KEY (conta_origem_id) REFERENCES contas(id),
    CONSTRAINT fk_transacao_conta_destino FOREIGN KEY (conta_destino_id) REFERENCES contas(id)
);
