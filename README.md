# 🏦 VaultPay API — Núcleo de Carteira Digital e Transações

[![Deploy on Render](https://img.shields.io/badge/Deploy-Render-46E3B7?style=for-the-badge&logo=render&logoColor=white)](https://vaultpay-backend-fur8.onrender.com/swagger-ui/index.html)
[![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)](https://vaultpay-backend-fur8.onrender.com/swagger-ui/index.html)

![Java](https://img.shields.io/badge/Java%2021-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot%203-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=spring-security&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=JSON%20web%20tokens&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2CA5E0?style=for-the-badge&logo=docker&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=for-the-badge&logo=flyway&logoColor=white)

> 🟢 **API AO VIVO:** [Teste a API agora mesmo através da documentação do Swagger clicando aqui](https://vaultpay-backend-fur8.onrender.com/swagger-ui/index.html). O deploy foi feito no Render (pode demorar alguns segundos para "acordar" no primeiro acesso).

O **VaultPay API** é uma API RESTful desenvolvida do zero para simular o núcleo (core banking) de uma carteira digital, resolvendo problemas críticos como **concorrência**, **condições de corrida** e **integridade de dados**.

---

## 📑 Índice
- [Funcionalidades](#-funcionalidades)
- [Tecnologias](#️-tecnologias-utilizadas)
- [Desafios Técnicos Resolvidos](#️-desafios-técnicos-resolvidos-engenharia--appsec)
- [Como Executar o Projeto](#-como-executar-o-projeto)
- [Endpoints Principais](#-endpoints-principais)
- [Autor](#-autor)

---

## 🚀 Funcionalidades

- **Abertura de Conta Segura:** A criação de um utilizador gera instantaneamente uma `Conta` associada com saldo zero, garantindo a atomicidade da operação.
- **Transferências Financeiras (ACID):** Movimentação de saldos entre contas garantindo que o dinheiro nunca se perde nem é duplicado em caso de falha (Rollbacks automáticos).
- **Extrato Imutável (Ledger):** Registro histórico de todas as transações (Depósitos e Transferências) utilizando identificadores únicos (UUIDs). Nenhuma transação pode ser editada ou apagada.
- **Segurança de Acesso e RBAC:** Autenticação via JWT (JSON Web Tokens) com controle de perfis de acesso, onde apenas os donos das contas podem transferir seus fundos.

---

## 🛠️ Tecnologias Utilizadas

- **Linguagem:** Java 21 (com Virtual Threads habilitadas)
- **Framework:** Spring Boot 3
- **Segurança:** Spring Security & JWT
- **Banco de Dados:** PostgreSQL
- **Infraestrutura:** Docker (Containerização)
- **Migrations:** Flyway
- **Rate Limiting:** Bucket4j & Caffeine Cache

---

## 🛡️ Desafios Técnicos Resolvidos (Engenharia & AppSec)

Além de manter a robustez de segurança contra vulnerabilidades (OWASP), este projeto implementa soluções vitais para *fintechs*:

- **Defesa contra Race Conditions (Pessimistic Locking):** Implementação de `@Lock(LockModeType.PESSIMISTIC_WRITE)` no banco de dados. Evita que duas transferências simultâneas na mesma conta leiam o mesmo saldo base, prevenindo a duplicação indevida de fundos.
- **Transações ACID Rigorosas:** Uso profundo de `@Transactional` nos serviços financeiros. Se o débito na origem ocorrer mas o crédito no destino falhar, toda a operação é revertida instantaneamente.
- **Alta Concorrência com Virtual Threads:** Configuração para utilizar Virtual Threads do Java 21 (`spring.threads.virtual.enabled=true`), permitindo milhares de transações simultâneas com baixo consumo de RAM.
- **Proteção contra Força Bruta e Spam:** Rate Limiting granular isolando endpoints críticos, impedindo ataques de negação de serviço.
- **Tratamento de Exceções Semânticas:** Respostas HTTP claras (ex: `422 Unprocessable Entity` para saldos insuficientes), ocultando stack traces e não expondo a arquitetura interna.

---

## ⚙️ Como Executar o Projeto

### Pré-requisitos
- [Java 21](https://jdk.java.net/21/)
- [Docker](https://www.docker.com/) e Docker Compose
- Maven

### Passo a Passo

1. **Clone o repositório**
```bash
git clone https://github.com/u-santos1/vaultpay-backend.git
cd vaultpay-backend
```

2. **Opção 1: Rodar tudo 100% via Docker (Recomendado)**
```bash
# Isso vai compilar a API, subir o Banco de Dados e conectar os dois automaticamente
docker-compose up -d --build
```
> A API estará rodando em: `http://localhost:8080/swagger-ui/index.html`

3. **Opção 2: Rodar Banco via Docker e API via Maven (Modo Dev)**
```bash
# Suba apenas o banco de dados
docker-compose up db -d

# Execute a aplicação via Maven
mvn spring-boot:run
```

---

## 📡 Endpoints Principais

Abaixo estão as rotas mais importantes da API:

### 👤 Usuários & Autenticação
- `POST /auth/login` - Autentica usuário e retorna o token JWT.

### 💳 Contas
- `POST /contas` - Cria uma nova conta bancária (Cria usuário e conta via transação).
- `GET /contas/{id}` - Retorna dados e saldo da conta.

### 💸 Transações
- `POST /transacoes/transferir` - Realiza uma transferência entre contas.
- `POST /transacoes/depositar` - Realiza um depósito na conta (Apenas admin ou sistema interno).
- `GET /transacoes/{contaId}/extrato` - Retorna o extrato de movimentações.

*(Observação: Todas as requisições de contas e transações necessitam do envio do Header `Authorization: Bearer <seu-token-jwt>`)*

---

## 👨‍💻 Autor

**Uerles Santos**  
*Java Backend Developer in training*  
[🔗 LinkedIn](https://www.linkedin.com/in/) | [🐙 GitHub](https://github.com/u-santos1)
