# CP5 Fase 2C — execucao local

Este roteiro executa o fluxo completo:

```text
Mercado -> RabbitMQ -> Payment Service -> Banco Tranquilo
        <- RabbitMQ <- Payment Service
Mercado -> Mailpit
```

Mailpit captura mensagens somente na maquina local. Ele nao entrega email real.

## 1. Infraestrutura

O Compose fica no projeto do servico de pagamento e inicia RabbitMQ Management, PostgreSQL local e Mailpit:

```powershell
cd D:\Programming\FIAP\2_Ano\mercado-payment-service
docker compose up -d
docker compose ps
```

Se necessario, substitua `docker compose` por `docker-compose`.

| Servico | Endereco local |
| --- | --- |
| RabbitMQ AMQP | `localhost:5672` |
| RabbitMQ Management | `http://localhost:15672` (`guest` / `guest`) |
| PostgreSQL | `localhost:5433` |
| Mailpit SMTP | `localhost:1025` |
| Mailpit UI/API | `http://localhost:8025` |

Os valores PostgreSQL do Compose sao credenciais exclusivas de desenvolvimento local.

## 2. Aplicacoes

Terminal 1 — Banco Tranquilo:

```powershell
cd D:\Programming\FIAP\2_Ano\banco-tranquilo-simulator
.\mvnw.cmd spring-boot:run
```

Terminal 2 — Payment Service:

```powershell
cd D:\Programming\FIAP\2_Ano\mercado-payment-service
.\mvnw.cmd spring-boot:run
```

Terminal 3 — Mercado:

```powershell
cd D:\Programming\FIAP\2_Ano\mercado-api-II
$env:SPRING_PROFILES_ACTIVE="postgres"
$env:DB_URL="jdbc:postgresql://localhost:5433/mercado_cp5"
$env:DB_USERNAME="mercado_cp5_app"
$env:DB_PASSWORD="mercado_cp5_local"
$env:APP_ADMIN_USERNAME="admin.local"
$env:APP_ADMIN_PASSWORD="admin-local-123"
$env:APP_ADMIN_NAME="Administrador Local"
$env:RABBITMQ_HOST="localhost"
$env:MAIL_HOST="localhost"
$env:MAIL_PORT="1025"
$env:EMAIL_TRANSPORT="smtp"
.\mvnw.cmd spring-boot:run
```

A V4 e aplicada somente ao PostgreSQL local desse Compose. Este roteiro nao aponta para Render ou qualquer banco externo.

## 3. Demonstracao

1. Acesse `http://localhost:8082/registro` e crie um cliente com um email valido, por exemplo `cliente@local.test`. Login e email sao campos independentes.
2. Entre como `admin.local`, crie produtos com estoque e custo.
3. Entre como o cliente e crie compras por `POST /api/compras` usando uma sessao autenticada.
4. O simulador aprova `purchaseId` impar e rejeita `purchaseId` par. Assim, em um banco vazio, a primeira compra demonstra `APPROVED` e a segunda `REJECTED`.
5. Consulte as compras em `GET /api/compras` para ver os estados terminais.
6. Abra Mailpit em `http://localhost:8025`: somente a compra aprovada gera mensagem.
7. Abra RabbitMQ Management e confirme os exchanges, bindings e consumidores, incluindo `mercado.payment.requests`, `mercado.payment.results`, retries e DLQs.

O outbox pode ser auditado no PostgreSQL local:

```sql
SELECT COMPRA_ID, PAYMENT_ATTEMPT_ID, STATUS, TENTATIVAS, ULTIMO_ERRO
FROM CP5_PAYMENT_OUTBOX
ORDER BY ID;

SELECT COMPRA_ID, DESTINATARIO, TIPO, STATUS, TENTATIVAS,
       ULTIMA_TENTATIVA_EM, ENVIADO_EM, ULTIMO_ERRO
FROM CP5_EMAIL_NOTIFICATION
ORDER BY ID;
```

## 4. Transporte HTTPS futuro no Render

O Render Free nao precisa de acesso SMTP direto. Depois que um provedor HTTPS for aprovado, configure secrets somente no ambiente:

```text
EMAIL_TRANSPORT=https-api
EMAIL_API_URL=https://<endpoint-do-provedor>
EMAIL_API_TOKEN=<secret-do-provedor>
EMAIL_FROM=<remetente-verificado>
EMAIL_API_TIMEOUT=5s
```

O adaptador atual envia `from`, `to`, `subject` e `text` em JSON com `Authorization: Bearer`. O contrato deve ser ajustado ao provedor escolhido antes do deploy. Nenhum provedor externo foi configurado ou chamado nesta fase.

## 5. Encerramento

```powershell
cd D:\Programming\FIAP\2_Ano\mercado-payment-service
docker compose down
```

O comando preserva os volumes locais. `docker compose down -v` apaga os bancos e filas locais e deve ser usado somente quando essa perda for intencional.
