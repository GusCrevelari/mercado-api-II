# Mercado Express

## Visão geral

Mercado Express é uma aplicação Java para gerenciamento de produtos, estoque, compras e pagamentos. A interface web utiliza Spring MVC e Thymeleaf, enquanto APIs REST permitem consultar produtos e operar o fluxo de compras. Autenticação e autorização são persistidas no banco de dados, com permissões distintas para administradores e clientes.

O processamento financeiro é assíncrono: a aplicação publica solicitações no RabbitMQ, um microsserviço independente consulta o Banco Tranquilo — uma API REST simulada para fins acadêmicos — e o resultado retorna ao Mercado Express. Compras aprovadas geram uma confirmação por e-mail e alimentam um dashboard administrativo com indicadores e gráficos.

## Principais funcionalidades

- CRUD de produtos com Spring MVC, Thymeleaf e validação de formulários.
- API REST de produtos com respostas HATEOAS.
- Login personalizado com Spring Security e senhas protegidas por BCrypt.
- Cadastro de clientes com login e endereço de e-mail distintos.
- Perfis `ADMIN` e `CLIENTE`, com autorização aplicada no servidor.
- Controle de quantidade em estoque, preço de venda e custo de aquisição.
- Criação transacional de compras e reserva de estoque com bloqueio pessimista.
- Snapshots imutáveis de nome, preço e custo dos itens comprados.
- Integração assíncrona de pagamentos com RabbitMQ e outbox transacional.
- Tratamento idempotente de resultados `APPROVED` e `REJECTED`.
- Liberação automática da reserva de estoque em pagamentos rejeitados.
- Confirmação de pagamento por e-mail e histórico de tentativas no PostgreSQL.
- Dashboard exclusivo para administradores, com métricas financeiras, inventário, compras e gráficos SVG.

## Arquitetura e fluxo de pagamento

Os três projetos são aplicações Spring Boot independentes. O Mercado Express mantém o domínio de produtos, usuários, compras, estoque, notificações e dashboard; o Payment Service integra a mensageria ao simulador bancário.

```text
CLIENTE
   |
   | POST /api/compras
   v
Mercado API II ---- transação ----> PostgreSQL
   |                                  | compra + itens
   |                                  | reserva de estoque
   |                                  | payment outbox
   |
   +---- RabbitMQ: payment.request ----> Mercado Payment Service
                                              |
                                              | REST local
                                              v
                                   Banco Tranquilo Simulator
                                              |
   <---- RabbitMQ: payment.result <-----------+
   |
   +---- APPROVED: finaliza compra e agenda confirmação por e-mail
   +---- REJECTED: finaliza compra e restaura a reserva de estoque
   |
   +---- PostgreSQL: histórico de notificação
   +---- Mailpit local: captura da mensagem

ADMIN ----> Dashboard MVC ----> receita, lucro, compras e estoque
```

A criação da compra e do registro de outbox ocorre na mesma transação. A publicação só é marcada como concluída após a confirmação do broker, mantendo o mesmo `paymentAttemptId` em novas tentativas. O consumidor valida compra, correlação, valor e moeda antes de aplicar o resultado. Mensagens duplicadas não alteram o estoque nem geram uma segunda notificação.

## Tecnologias utilizadas

- Java 17 e Maven Wrapper.
- Spring Boot 4.1.1 no Mercado Express.
- Spring MVC, Thymeleaf e Thymeleaf Spring Security Extras.
- Spring Security e BCrypt.
- Spring Data JPA, Hibernate e Bean Validation.
- Spring HATEOAS.
- Spring AMQP e RabbitMQ Management.
- Spring Mail, SMTP local e transporte configurável por API HTTPS.
- PostgreSQL 17 e Flyway.
- H2 nos testes automatizados.
- Mailpit para captura local de e-mails.
- Docker e Docker Compose para a infraestrutura local.
- Lombok.
- Oracle JDBC para compatibilidade com o ambiente legado de produtos.

Configuração inicial do projeto:

![Configuração do projeto no Spring Initializr](docs/images/spring-initializr.png)

## Perfis e permissões

| Recurso | ADMIN | CLIENTE |
| --- | --- | --- |
| Consultar produtos e estoque | Sim | Sim |
| Criar, editar e excluir produtos | Sim | Não |
| Consultar custo de aquisição na interface | Sim | Não |
| Criar compra | Não | Sim |
| Consultar compras | Todas | Somente as próprias |
| Acessar o dashboard financeiro | Sim | Não |
| Criar conta pelo formulário público | Não aplicável | Sim |

O cadastro público sempre cria usuários `CLIENTE`. A conta administrativa pode ser provisionada na inicialização por variáveis de ambiente e nunca é criada pelo formulário público. Rotas MVC permanecem protegidas por CSRF; as rotas JSON `/api/**` exigem sessão autenticada, mas são isentas de CSRF para clientes como o Postman.

## Endpoints REST e exemplo de compra

| Método | Rota | Acesso | Finalidade |
| --- | --- | --- | --- |
| `GET` | `/api/mercado` | ADMIN, CLIENTE | Lista produtos e estoque. |
| `GET` | `/api/mercado/{id}` | ADMIN, CLIENTE | Consulta um produto. |
| `POST` | `/api/mercado` | ADMIN | Cadastra um produto. |
| `PUT` | `/api/mercado/{id}` | ADMIN | Substitui os dados do produto. |
| `PATCH` | `/api/mercado/{id}` | ADMIN | Atualiza campos do produto. |
| `DELETE` | `/api/mercado/{id}` | ADMIN | Exclui um produto. |
| `POST` | `/api/compras` | CLIENTE | Cria a compra e reserva o estoque. |
| `GET` | `/api/compras` | ADMIN, CLIENTE | ADMIN lista todas; CLIENTE lista somente as próprias. |
| `GET` | `/api/compras/{id}` | ADMIN, CLIENTE | Consulta uma compra respeitando a propriedade do cliente. |

O servidor identifica o cliente pela sessão e obtém preço e custo diretamente do banco. O corpo não recebe login, usuário, preço ou total:

```json
{
  "itens": [
    {
      "produtoId": 1,
      "quantidade": 2
    },
    {
      "produtoId": 2,
      "quantidade": 1
    }
  ]
}
```

A resposta `201 Created` contém os itens, total calculado pelo servidor, moeda `BRL`, status inicial `PENDING_PAYMENT` e `correlationId`.

Principais rotas MVC:

| Método | Rota | Acesso |
| --- | --- | --- |
| `GET` | `/`, `/login`, `/registro` | Público |
| `POST` | `/registro` | Público, perfil PostgreSQL |
| `GET` | `/mercado`, `/mercado/{id}` | ADMIN, CLIENTE |
| `GET` | `/mercado/novo`, `/mercado/{id}/editar` | ADMIN |
| `POST/PUT/DELETE` | Criação, atualização e exclusão em `/mercado/**` | ADMIN |
| `GET` | `/admin/dashboard` | ADMIN |

## PostgreSQL e Flyway

O perfil `postgres` usa `ddl-auto=validate`; toda alteração estrutural é controlada pelo Flyway:

| Migration | Estrutura principal |
| --- | --- |
| `V1__create_product_schema.sql` | Produto, preço e sequência. |
| `V2__create_user_schema.sql` | Usuários, login, senha, estado e perfil. |
| `V3__create_purchase_and_inventory_schema.sql` | Estoque, custo de aquisição, compras, itens, snapshots, status e correlação. |
| `V4__create_payment_outbox_and_email_notification.sql` | E-mail do usuário, outbox de pagamento e histórico de notificações. |

Tabelas principais:

- `TDS_MVC_TB_MERCADO`: cadastro, preço, custo e estoque atual.
- `CP5_USUARIO`: identidade, e-mail, credencial BCrypt e perfil.
- `CP5_COMPRA`: cliente, totais, moeda, status, correlação e datas.
- `CP5_COMPRA_ITEM`: quantidades e snapshots de venda e aquisição.
- `CP5_PAYMENT_OUTBOX`: tentativa estável de pagamento, estado de publicação e erros.
- `CP5_EMAIL_NOTIFICATION`: destinatário, tipo, estado, tentativas, envio e falha.

O perfil `oracle` mantém compatibilidade com a tabela legada de produtos descrita em [`docs/database/oracle-schema.sql`](docs/database/oracle-schema.sql). Nesse perfil o Flyway permanece desabilitado e as credenciais são fornecidas exclusivamente por ambiente.

## Instalação e execução local

### Pré-requisitos

- JDK 17 configurado em `JAVA_HOME`.
- Docker Desktop com Docker Compose.
- Git.

Clone os três repositórios no mesmo diretório pai:

```powershell
git clone https://github.com/GusCrevelari/mercado-api-II.git
git clone https://github.com/GusCrevelari/mercado-payment-service.git
git clone https://github.com/GusCrevelari/banco-tranquilo-simulator.git
```

O Compose está no repositório `mercado-payment-service`. Configure credenciais apenas na sessão local e inicie PostgreSQL, RabbitMQ e Mailpit:

```powershell
cd mercado-payment-service
$env:POSTGRES_DB="mercado_cp5"
$env:POSTGRES_USER="mercado_cp5_app"
$env:POSTGRES_PASSWORD="<senha-postgres-local>"
$env:RABBITMQ_USERNAME="<usuario-rabbit-local>"
$env:RABBITMQ_PASSWORD="<senha-rabbit-local>"
docker compose up -d
docker compose ps
```

Terminal do Banco Tranquilo:

```powershell
cd banco-tranquilo-simulator
$env:PORT="8091"
.\mvnw.cmd spring-boot:run
```

Terminal do Payment Service:

```powershell
cd mercado-payment-service
$env:RABBITMQ_HOST="localhost"
$env:RABBITMQ_PORT="5672"
$env:RABBITMQ_USERNAME="<usuario-rabbit-local>"
$env:RABBITMQ_PASSWORD="<senha-rabbit-local>"
$env:BANCO_TRANQUILO_URL="http://localhost:8091"
.\mvnw.cmd spring-boot:run
```

Terminal do Mercado Express:

```powershell
cd mercado-api-II
$env:SPRING_PROFILES_ACTIVE="postgres"
$env:DB_URL="jdbc:postgresql://localhost:5433/mercado_cp5"
$env:DB_USERNAME="mercado_cp5_app"
$env:DB_PASSWORD="<senha-postgres-local>"
$env:APP_ADMIN_USERNAME="<admin-local>"
$env:APP_ADMIN_PASSWORD="<senha-admin-local>"
$env:APP_ADMIN_NAME="Administrador Local"
$env:RABBITMQ_HOST="localhost"
$env:RABBITMQ_PORT="5672"
$env:RABBITMQ_USERNAME="<usuario-rabbit-local>"
$env:RABBITMQ_PASSWORD="<senha-rabbit-local>"
$env:EMAIL_TRANSPORT="smtp"
$env:MAIL_HOST="localhost"
$env:MAIL_PORT="1025"
.\mvnw.cmd spring-boot:run
```

Serviços locais:

| Serviço | Endereço |
| --- | --- |
| Mercado Express | `http://localhost:8082` |
| Banco Tranquilo | `http://localhost:8091` |
| RabbitMQ AMQP | `localhost:5672` |
| RabbitMQ Management | `http://localhost:15672` |
| PostgreSQL | `localhost:5433` |
| Mailpit | `http://localhost:8025` |

## RabbitMQ, Banco Tranquilo e confirmação de pagamento

O RabbitMQ utiliza exchanges, filas e mensagens duráveis:

| Tipo | Nome | Routing key |
| --- | --- | --- |
| Exchange principal | `mercado.payments` | `payment.request`, `payment.result` |
| Fila de solicitações | `mercado.payment.requests` | `payment.request` |
| Fila de resultados | `mercado.payment.results` | `payment.result` |
| Exchange de retry | `mercado.payments.retry` | `payment.request.retry` |
| Fila de retry | `mercado.payment.requests.retry` | `payment.request.retry` |
| Dead-letter exchange | `mercado.payments.dlx` | `payment.request.dead`, `payment.result.dead` |
| Filas de dead letter | `mercado.payment.requests.dlq`, `mercado.payment.results.dlq` | Conforme a origem |

O Banco Tranquilo é um simulador acadêmico de autorização de pagamento disponível localmente em `POST /api/v1/payments`. Para resultados reproduzíveis, compras com ID ímpar recebem `APPROVED` e compras com ID par recebem `REJECTED`.

Em `APPROVED`, a reserva original é mantida, a compra é finalizada e uma notificação `PAYMENT_APPROVED` é criada. Em `REJECTED`, o estoque reservado é devolvido e nenhum e-mail de sucesso é gerado. Resultados inválidos ou conflitantes seguem para a DLQ de resultados.

O transporte de e-mail é configurável:

- `smtp`: usado localmente com Mailpit em `localhost:1025`;
- `https-api`: adaptador para um provedor futuro, exigindo endpoint HTTPS e token por variáveis de ambiente.

O Mailpit apenas captura mensagens locais para inspeção em `http://localhost:8025`; ele não entrega e-mails a caixas postais externas. Nenhum provedor externo ou credencial real está configurado no projeto.

## Dashboard administrativo

O dashboard está disponível em `GET /admin/dashboard` somente para `ADMIN` e apresenta:

- receita total de compras `APPROVED`;
- lucro total calculado por `(preço unitário - custo unitário) × quantidade`;
- quantidade de compras aprovadas, pendentes e rejeitadas;
- estoque atual por produto;
- histórico com compra, cliente, data, total e status;
- gráfico SVG de receita e lucro;
- gráfico SVG de estoque por produto;
- estados vazios quando não há dados.

Receita e lucro ignoram compras `PENDING_PAYMENT` e `REJECTED`. O lucro usa exclusivamente os snapshots gravados nos itens, portanto alterações posteriores no cadastro do produto não modificam o resultado histórico. Todos os cálculos monetários são executados no servidor com `BigDecimal`.

## Testes automatizados e Postman

Execute a suíte completa:

```powershell
.\mvnw.cmd clean package
```

A suíte do Mercado Express contém 32 testes que cobrem cadastro, autenticação, autorização, CRUD, compras, reserva e liberação de estoque, contratos JSON, outbox, resultados duplicados, falhas de broker/e-mail, notificações e dashboard.

A coleção [`postman/mercado-express-e2e.postman_collection.json`](postman/mercado-express-e2e.postman_collection.json) contém o fluxo de login por sessão, obtenção do token CSRF, manutenção do cookie `JSESSIONID`, consulta de produtos, criação/consulta de compras e acesso administrativo ao dashboard.

## Capturas da interface

### Página inicial e autenticação

![Página inicial do Mercado Express](docs/images/2-home.png)

![Login personalizado](docs/images/1-login.png)

### Produtos

![Listagem de produtos](docs/images/03-produtos.png)

![Cadastro de produto](docs/images/05-novo-produto.png)

![Produto cadastrado](docs/images/05-produto-criado.png)

![Detalhes do produto](docs/images/06-detalhes.png)

![Edição de produto](docs/images/07-editar-produto.png)

![Produto atualizado](docs/images/08-produto-atualizado.png)

## Deploy existente no Render

A interface Spring MVC está publicada em:

**https://mercado-api-ii.onrender.com**

Esse ambiente é destinado à demonstração de autenticação e gerenciamento de produtos. O RabbitMQ, o Payment Service, o Banco Tranquilo e o Mailpit não estão implantados no Render; o fluxo completo de pagamento e confirmação por e-mail é executado e validado localmente.

## Repositórios relacionados

- [Mercado API II](https://github.com/GusCrevelari/mercado-api-II): aplicação principal, domínio de compras, estoque, notificações e dashboard.
- [Mercado Payment Service](https://github.com/GusCrevelari/mercado-payment-service): consumidor RabbitMQ, integração bancária, retry, dead letter e publicação de resultados.
- [Banco Tranquilo Simulator](https://github.com/GusCrevelari/banco-tranquilo-simulator): simulador acadêmico da API REST de autorização de pagamentos.

Os microsserviços permanecem projetos Java independentes e são executados separadamente.
