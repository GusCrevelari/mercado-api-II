# CP5 Fase 2E — demonstracao local

Este roteiro demonstra somente infraestrutura local:

```text
Mercado -> RabbitMQ -> Payment Service -> Banco Tranquilo
        <- RabbitMQ <- Payment Service
Mercado -> Mailpit e Dashboard ADMIN
```

O Mailpit captura a mensagem na maquina local. Isso nao representa entrega em uma caixa de email real.

## 1. Build e infraestrutura

Em tres terminais, execute os builds:

```powershell
cd D:\Programming\FIAP\2_Ano\mercado-api-II
.\mvnw.cmd clean package

cd D:\Programming\FIAP\2_Ano\mercado-payment-service
.\mvnw.cmd clean package

cd D:\Programming\FIAP\2_Ano\banco-tranquilo-simulator
.\mvnw.cmd clean package
```

Inicie a infraestrutura definida no repositorio do Payment Service:

```powershell
cd D:\Programming\FIAP\2_Ano\mercado-payment-service
docker compose up -d
docker compose ps
```

| Componente | Endereco local |
| --- | --- |
| Mercado API II | `http://localhost:8082` |
| Dashboard ADMIN | `http://localhost:8082/admin/dashboard` |
| Banco Tranquilo | `http://localhost:8091` |
| RabbitMQ AMQP | `localhost:5672` |
| RabbitMQ Management | `http://localhost:15672` — `guest` / `guest` |
| PostgreSQL | `localhost:5433` |
| Mailpit SMTP | `localhost:1025` |
| Mailpit UI | `http://localhost:8025` |

As senhas abaixo sao apenas credenciais locais de demonstracao definidas pelo Compose ou pela sessao do terminal.

## 2. Aplicacoes

Confirme que `JAVA_HOME` aponta para um JDK 17 (`& "$env:JAVA_HOME\bin\java.exe" -version`). Os comandos abaixo usam esse executavel explicitamente, mesmo que outra versao de `java` apareca antes no `PATH`.

Terminal Banco Tranquilo:

```powershell
cd D:\Programming\FIAP\2_Ano\banco-tranquilo-simulator
$env:PORT="8091"
& "$env:JAVA_HOME\bin\java.exe" -jar target\banco-tranquilo-simulator-0.0.1-SNAPSHOT.jar
```

Terminal Payment Service:

```powershell
cd D:\Programming\FIAP\2_Ano\mercado-payment-service
$env:RABBITMQ_HOST="localhost"
$env:RABBITMQ_PORT="5672"
$env:RABBITMQ_USERNAME="guest"
$env:RABBITMQ_PASSWORD="guest"
$env:BANCO_TRANQUILO_URL="http://localhost:8091"
& "$env:JAVA_HOME\bin\java.exe" -jar target\mercado-payment-service-0.0.1-SNAPSHOT.jar
```

Terminal Mercado:

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
$env:RABBITMQ_PORT="5672"
$env:RABBITMQ_USERNAME="guest"
$env:RABBITMQ_PASSWORD="guest"
$env:EMAIL_TRANSPORT="smtp"
$env:MAIL_HOST="localhost"
$env:MAIL_PORT="1025"
& "$env:JAVA_HOME\bin\java.exe" -jar target\mercado-mvc-0.0.1-SNAPSHOT.jar
```

O bootstrap cria o ADMIN apenas se `admin.local` ainda nao existir; ele nunca troca uma senha existente. Crie o CLIENTE em `http://localhost:8082/registro`, informando nome, login, senha e um email local valido, por exemplo `cliente.integracao@example.test`. Login e email sao campos distintos.

## 3. Postman e sessao

Importe `postman/phase-2e-e2e.postman_collection.json`. Deixe o Cookie Jar habilitado e execute, em ordem, as requisicoes da pasta `CLIENTE`:

1. `01 - Obter CSRF do login` grava `csrfToken` e recebe `JSESSIONID`.
2. `02 - Autenticar CLIENTE` envia formulario `x-www-form-urlencoded`; o Postman mantem a sessao no cookie.
3. `03 - Listar produtos` confirma o produto e o estoque.
4. `04 - Criar compra` grava o ID retornado em `purchaseId`.
5. `05 - Consultar compra` mostra o estado terminal.

Para entrar como ADMIN, limpe o cookie `JSESSIONID` de `localhost` no Cookie Jar e execute a pasta `ADMIN`. As variaveis de credenciais sao valores locais de exemplo e podem ser trocadas na colecao.

Corpo da compra:

```json
{
  "itens": [
    {
      "produtoId": 1,
      "quantidade": 1
    }
  ]
}
```

As rotas JSON `/api/**` ignoram CSRF, mas exigem o cookie de uma sessao autenticada. Os formularios MVC continuam protegidos por CSRF.

## 4. IDs deterministas

O Banco Tranquilo aprova `purchaseId` impar e rejeita `purchaseId` par. Nao suponha que o proximo ID seja 1. Antes da gravacao, consulte a sequence:

```powershell
docker exec fiap-mercado-postgres psql -U mercado_cp5_app -d mercado_cp5 -c "SELECT CASE WHEN is_called THEN last_value + 1 ELSE last_value END AS next_purchase_id FROM cp5_seq_compra;"
```

Em uma demonstracao sem outras requisicoes concorrentes:

- se o proximo ID for impar, crie a compra APPROVED e depois a REJECTED;
- se for par, crie primeiro uma compra de alinhamento, aguarde seu REJECTED, e entao crie as duas compras da demonstracao.

Anote o estoque inicial. Uma compra aprovada de uma unidade reduz o estoque em uma. Uma compra rejeitada pode aparecer brevemente como reserva, mas deve restaurar toda a sua quantidade.

## 5. Evidencias

No RabbitMQ Management, abra `Exchanges`, `Queues and Streams` e `Connections`. Devem existir os exchanges duraveis `mercado.payments`, `mercado.payments.retry` e `mercado.payments.dlx`; as filas duraveis de request, retry, request DLQ, result e result DLQ; um consumidor de requests e um consumidor de results.

Consultas PostgreSQL locais:

```powershell
docker exec fiap-mercado-postgres psql -U mercado_cp5_app -d mercado_cp5 -c "SELECT c.id, u.login, c.criado_em, c.total, c.status, c.correlation_id FROM cp5_compra c JOIN cp5_usuario u ON u.id=c.cliente_id ORDER BY c.id DESC;"

docker exec fiap-mercado-postgres psql -U mercado_cp5_app -d mercado_cp5 -c "SELECT compra_id, payment_attempt_id, status, tentativas, publicado_em, ultimo_erro FROM cp5_payment_outbox ORDER BY id DESC;"

docker exec fiap-mercado-postgres psql -U mercado_cp5_app -d mercado_cp5 -c "SELECT compra_id, destinatario, tipo, status, tentativas, ultima_tentativa_em, enviado_em, ultimo_erro FROM cp5_email_notification ORDER BY id DESC;"

docker exec fiap-mercado-postgres psql -U mercado_cp5_app -d mercado_cp5 -c "SELECT id, nome, quantidade_estoque, preco, custo_aquisicao FROM tds_mvc_tb_mercado ORDER BY id;"

docker exec fiap-mercado-postgres psql -U mercado_cp5_app -d mercado_cp5 -c "SELECT COALESCE(SUM(c.total),0) AS receita_aprovada, COALESCE(SUM(ci.quantidade * (ci.preco_unitario-ci.custo_unitario)),0) AS lucro_aprovado FROM cp5_compra c LEFT JOIN cp5_compra_item ci ON ci.compra_id=c.id WHERE c.status='APPROVED';"
```

Logs esperados:

- Mercado: `Solicitacao de pagamento publicada` com compra, `paymentAttemptId` e `correlationId`;
- Payment Service: `Pagamento ... processado; duplicate=false`;
- Mercado: `Resultado do pagamento ... processado: APPLIED`;
- Mercado, somente para APPROVED: `Email de confirmacao aceito pelo transporte`;
- duplicata: `Resultado do pagamento ... processado: DUPLICATE_IGNORED`.

Abra `http://localhost:8025`: deve haver exatamente uma nova mensagem para a compra aprovada e nenhuma para a rejeitada. A mensagem foi aceita e capturada pelo Mailpit local, nao entregue na internet.

Entre como ADMIN em `http://localhost:8082/login` e abra `/admin/dashboard`. Confira as duas compras, receita/lucro somente de APPROVED e os dois SVGs. Entre como CLIENTE em outra sessao e confirme `403` ao acessar `/admin/dashboard`.

## 6. Duplicata controlada

O trecho abaixo republica o ultimo resultado APPROVED no RabbitMQ local. Execute somente depois da compra ter terminado:

```powershell
$row = docker exec fiap-mercado-postgres psql -U mercado_cp5_app -d mercado_cp5 -At -F "|" -c "SELECT o.payment_attempt_id,c.id,c.correlation_id,c.total,c.moeda FROM cp5_compra c JOIN cp5_payment_outbox o ON o.compra_id=c.id WHERE c.status='APPROVED' ORDER BY c.id DESC LIMIT 1;"
$attemptId,$purchaseId,$correlationId,$amount,$currency = $row.Trim() -split '\|'
$envelope = @{paymentAttemptId=$attemptId; payload=@{purchaseId=[long]$purchaseId; correlationId=$correlationId; amount=[decimal]$amount; currency=$currency; status='APPROVED'}} | ConvertTo-Json -Compress -Depth 5
$auth = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes('guest:guest'))
$publish = @{properties=@{delivery_mode=2; content_type='application/json'}; routing_key='payment.result'; payload=$envelope; payload_encoding='string'} | ConvertTo-Json -Compress -Depth 6
Invoke-RestMethod -Uri 'http://localhost:15672/api/exchanges/%2f/mercado.payments/publish' -Method Post -Headers @{Authorization="Basic $auth"} -ContentType 'application/json' -Body $publish
```

O retorno deve conter `routed: true`. Confirme que o estoque nao mudou, a notificacao continua unica, `tentativas` continua 1 e o Mailpit continua com apenas uma mensagem para essa compra.

## 7. Encerramento

Interrompa os tres processos Java com `Ctrl+C`. Para parar os containers sem apagar os volumes:

```powershell
cd D:\Programming\FIAP\2_Ano\mercado-payment-service
docker compose down
```

Nao use `docker compose down -v` durante a gravacao: essa opcao apaga os dados locais.
