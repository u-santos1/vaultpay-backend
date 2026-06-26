# 🏦 VaultPay API — Núcleo de Carteira Digital e Transações

![Java](https://img.shields.io/badge/Java%2021-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot%203-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=spring-security&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=JSON%20web%20tokens&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2CA5E0?style=for-the-badge&logo=docker&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=for-the-badge&logo=flyway&logoColor=white)

## 📌 O Que É

O **VaultPay API** é uma API RESTful desenvolvida do zero para simular o núcleo (core banking) de uma carteira digital. 

Diferente de um sistema CRUD tradicional, o domínio financeiro exige soluções robustas para problemas críticos como **concorrência**, **condições de corrida (race conditions)** e **integridade de dados**. Este projeto serve como um laboratório avançado para demonstrar arquitetura de software focada em transações financeiras imutáveis (Ledger), utilizando as mais recentes inovações de performance do **Java 21**.

## 🚀 Funcionalidades

- **Abertura de Conta Segura:** A criação de um utilizador gera instantaneamente uma `Conta` associada com saldo zero, garantindo a atomicidade da operação.
- **Transferências Financeiras (ACID):** Movimentação de saldos entre contas garantindo que o dinheiro nunca se perde nem é duplicado em caso de falha no servidor (Rollbacks automáticos).
- **Extrato Imutável (Ledger):** Registo histórico de todas as transações (Depósitos e Transferências) utilizando identificadores únicos (UUIDs). Nenhuma transação financeira pode ser editada ou apagada.
- **Segurança de Acesso e RBAC:** Autenticação via JWT (JSON Web Tokens) com controlo de perfis de acesso, onde apenas os donos das contas podem transferir os seus fundos.

## 🛠️ Tecnologias Utilizadas

- **Linguagem:** Java 21 (com Virtual Threads habilitadas)
- **Framework:** Spring Boot 3
- **Segurança:** Spring Security & JWT
- **Banco de Dados:** PostgreSQL
- **Infraestrutura:** Docker (Containerização)
- **Migrations:** Flyway
- **Rate Limiting:** Bucket4j & Caffeine Cache

## 🛡️ Desafios Técnicos Resolvidos (Engenharia & AppSec)

Além de manter a robustez de segurança contra vulnerabilidades (OWASP), este projeto implementa soluções vitais para *fintechs*:

- **Defesa contra Race Conditions (Pessimistic Locking):** Implementação de `@Lock(LockModeType.PESSIMISTIC_WRITE)` no banco de dados. Evita que duas transferências simultâneas na mesma conta leiam o mesmo saldo base, prevenindo a duplicação indevida de fundos e saldos negativos.
- **Transações ACID rigorosas:** Uso profundo de `@Transactional` nos serviços financeiros. Se o débito na origem ocorrer mas o crédito no destino falhar, toda a operação é revertida instantaneamente.
- **Alta Concorrência com Virtual Threads:** Configuração do ecossistema para utilizar as Virtual Threads do Java 21 (`spring.threads.virtual.enabled=true`), permitindo o processamento de milhares de transações em simultâneo com um consumo mínimo de memória.
- **Proteção contra Força Bruta e Spam:** Rate Limiting granular isolando endpoints críticos, impedindo ataques de negação de serviço e tentativas de exaustão massiva de recursos.
- **Tratamento de Exceções FinTech:** Respostas HTTP semânticas (ex: `422 Unprocessable Entity` para saldos insuficientes), ocultando *stack traces* e não expondo a arquitetura interna.
