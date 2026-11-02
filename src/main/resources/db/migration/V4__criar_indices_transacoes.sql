CREATE INDEX idx_transacoes_conta_origem_id ON transacoes USING btree (conta_origem_id);
CREATE INDEX idx_transacoes_conta_destino_id ON transacoes USING btree (conta_destino_id);
CREATE INDEX idx_transacoes_data_hora ON transacoes USING btree (data_hora);
