
ALTER TABLE contas 
ADD COLUMN limite_transacao NUMERIC(38,2) NOT NULL DEFAULT 10000.00;


ALTER TABLE transacoes 
ADD COLUMN chave_idempotencia VARCHAR(255);


ALTER TABLE transacoes 
ALTER COLUMN chave_idempotencia SET NOT NULL;

ALTER TABLE transacoes 
ADD CONSTRAINT uk_chave_idempotencia UNIQUE (chave_idempotencia);
