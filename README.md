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
> Para testar os endpoints protegidos: faça `POST /login`, copie o `token` e clique em **Authorize** no topo do Swagger.

O **VaultPay API** é uma API RESTful desenvolvida do zero para simular o núcleo (core banking) de uma carteira digital, resolvendo problemas críticos como **concorrência**, **condições de corrida** e **integridade de dados**.

---

## 📑 Índice
- [Funcionalidades](#-funcionalidades)
- [Tecnologias](#️-tecnologias-utilizadas)
- [Desafios Técnicos Resolvidos](#️-desafios-técnicos-resolvidos-engenharia--appsec)
- [Como Executar o Projeto](#️-como-executar-o-projeto)
- [Endpoints Principais](#-endpoints-principais)
- [Autor](#-autor)

---

## 🚀 Funcionalidades

- **Abertura de Conta Segura:** cada usuário possui no máximo uma conta (relação 1:1 garantida por constraint no banco). A conta nasce **sempre com saldo zero** e usuários comuns só podem abrir conta para si mesmos.
- **Transferências Financeiras (ACID):** movimentação de saldos entre contas garantindo que o dinheiro nunca se perde nem é duplicado em caso de falha (rollback automático).
- **Depósitos e Saques:** o depósito é restrito ao perfil `ADMIN`; o saque exige que a conta pertença ao usuário logado e respeita saldo e limite por transação.
- **Idempotência:** o header `X-Idempotency-Key` evita que uma operação repetida (ex.: retentativa de rede) seja processada duas vezes.
- **Extrato Imutável (Ledger):** histórico paginado de transações identificadas por UUID. Nenhuma transação é editada ou apagada.
- **Segurança de Acesso e RBAC:** autenticação via JWT com perfis `ADMIN` e `USER`; apenas o dono da conta movimenta seus fundos.

---

## 🛠️ Tecnologias Utilizadas

- **Linguagem:** Java 21 (com Virtual Threads habilitadas)
- **Framework:** Spring Boot 3.5
- **Segurança:** Spring Security & JWT (java-jwt)
- **Banco de Dados:** PostgreSQL
- **Infraestrutura:** Docker (build multi-stage) e Docker Compose
- **Migrations:** Flyway
- **Rate Limiting:** Bucket4j (algoritmo Token Bucket, em memória)
- **Documentação:** Swagger / OpenAPI (springdoc)
- **Testes:** JUnit 5, Mockito e H2
- **CI:** GitHub Actions (build, testes e build da imagem Docker)

---

## 🛡️ Desafios Técnicos Resolvidos (Engenharia & AppSec)

- **Defesa contra Race Conditions (Pessimistic Locking):** `@Lock(LockModeType.PESSIMISTIC_WRITE)` no banco. Evita que duas operações simultâneas na mesma conta leiam o mesmo saldo base, prevenindo a duplicação indevida de fundos.
- **Prevenção de Deadlock:** nas transferências, as contas são travadas sempre em ordem crescente de ID, de modo que operações A→B e B→A simultâneas não se bloqueiam mutuamente.
- **Transações ACID Rigorosas:** `@Transactional` nos serviços financeiros. Se o débito na origem ocorrer mas o crédito no destino falhar, toda a operação é revertida.
- **Idempotência em duas camadas:** verificação da chave no código + constraint `UNIQUE` no banco como garantia final.
- **JWT com revogação:** o filtro consulta o usuário a cada requisição; tokens emitidos antes da última troca de senha e usuários desativados são rejeitados.
- **Proteção contra BOLA/IDOR:** o sistema valida que a conta de origem pertence ao usuário autenticado.
- **Alta Concorrência com Virtual Threads:** `spring.threads.virtual.enabled=true` (Java 21).
- **Proteção contra Força Bruta e Spam:** Rate Limiting por IP (5 requisições por minuto, reposição gradual), retornando `429 Too Many Requests`.
- **Notificações desacopladas:** evento publicado na transferência e tratado de forma assíncrona somente após o commit (`@TransactionalEventListener(AFTER_COMMIT)`).
- **Tratamento de Exceções Semânticas:** respostas HTTP claras e sem stack trace: `400` (sintaxe inválida / requisição malformada), `401` (credenciais inválidas / token revogado), `403` (sem permissão / BOLA), `404` (conta não encontrada), `409` (idempotência duplicada) e `422` (regras de negócio: saldo insuficiente, conta inativa, limite excedido).

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

4. **Rodar os testes (48 testes unitários, integração e concorrência com CountDownLatch)**
```bash
./mvnw test
```

---

## 📡 Endpoints Principais

### 👤 Usuários & Autenticação
- `POST /usuarios` — cadastra um novo usuário e abre sua carteira digital automaticamente com saldo R$ 0,00 (público).
- `POST /login` — autentica o usuário e retorna o token JWT (público).

### 💳 Contas
- `GET /conta/me` — consulta os dados, número e saldo atual da carteira do usuário logado (via JWT).
- `POST /conta` — cria a conta de um usuário existente, com saldo inicial zero. Usuário comum só cria para si; `ADMIN` cria para qualquer usuário.

### 💸 Transações
- `POST /transacao` — transferência entre contas (exige `X-Idempotency-Key`).
- `POST /transacao/deposito` — depósito em uma conta (**apenas `ADMIN`**, exige `X-Idempotency-Key`).
- `POST /transacao/saque` — saque da conta do usuário logado (exige `X-Idempotency-Key`).
- `GET /transacao/extrato/{contaId}` — extrato paginado (`?page=0&size=10`).

*(Observação: todas as rotas, exceto `/usuarios`, `/login` e o Swagger, exigem o header `Authorization: Bearer <seu-token-jwt>`)*

---

## 👨‍💻 Autor

**Uerles Santos**  
*Java Backend Developer in training*  
[🐙 GitHub](https://github.com/u-santos1)
