# Mercado Express — Checkpoint 4 Parte II

## Sobre o projeto

Mercado Express e uma aplicacao Spring MVC desenvolvida para o **FIAP Java Advanced — Checkpoint 4 Parte II: Spring MVC, Security e Deploy**.

O sistema permite que usuarios autenticados gerenciem produtos de mercado por uma interface web renderizada com **Spring MVC + Thymeleaf**. Os dados sao persistidos em banco **Oracle** por meio de Spring Data JPA, e as rotas privadas sao protegidas com Spring Security.

## Requisitos atendidos

- Spring Boot 4.1.1
- Maven
- Java 17
- Spring MVC com `@Controller`
- Thymeleaf
- CRUD web completo
- Persistencia em Oracle
- Spring Security com rotas publicas e privadas
- Lombok
- Bean Validation
- Projeto preparado para deploy com Docker no Render
- API REST adicional com DTOs, validacao, tratamento de erros e HATEOAS

A API REST/HATEOAS e um complemento para demonstrar conceitos praticados na Parte I. Ela nao substitui a aplicacao MVC exigida na Parte II.

## Tecnologias utilizadas

- Java 17
- Spring Boot 4.1.1
- Spring Web MVC
- Thymeleaf
- Spring Data JPA
- Oracle JDBC Driver
- Spring Security
- Spring HATEOAS
- Bean Validation
- Lombok
- Maven
- Docker

## Arquitetura

Fluxo principal da aplicacao web:

```text
Browser
   ↓
Spring Security
   ↓
MVC Controller
   ↓
Service
   ↓
Repository
   ↓
Oracle
```

Renderizacao HTML:

```text
MVC Controller
   ↓
Thymeleaf
   ↓
HTML
```

Fluxo adicional da API REST:

```text
REST Client
   ↓
REST Controller
   ↓
Service
   ↓
Repository
   ↓
Oracle
```

## Banco de dados Oracle

O projeto usa os objetos Oracle definidos em [docs/database/oracle-schema.sql](docs/database/oracle-schema.sql).

Tabela:

```text
TDS_MVC_TB_MERCADO
```

Sequence:

```text
TDS_MVC_SEQ_MERCADO
```

Campos persistidos:

| Campo | Tipo Java | Observacao |
| ----- | --------- | ---------- |
| `id` | `Long` | Chave primaria gerada pela sequence |
| `nome` | `String` | Obrigatorio, ate 120 caracteres |
| `tipo` | `String` | Obrigatorio, ate 50 caracteres |
| `setor` | `String` | Obrigatorio, ate 80 caracteres |
| `tamanho` | `String` | Obrigatorio, ate 40 caracteres |
| `preco` | `BigDecimal` | Obrigatorio, maior que zero, escala de 2 casas |

A aplicacao usa `spring.jpa.hibernate.ddl-auto=validate`, portanto nao cria nem altera a estrutura do banco automaticamente.

## Seguranca com Spring Security

Rotas publicas:

- `/`
- `/login`
- assets estaticos em `/css/**`, `/images/**`, `/webjars/**` e `/favicon.ico`
- leitura REST em `GET /api/mercado` e `GET /api/mercado/{id}`

Rotas privadas:

- `/mercado/**`
- mutacoes REST em `/api/mercado/**`

A autenticacao usa login por formulario do Spring Security. O logout e feito por `POST /logout`, com token CSRF nos formularios Thymeleaf.

Credenciais padrao para desenvolvimento local:

```text
Usuario: admin
Senha: fiap123
```

Esses valores devem ser substituidos em ambiente de deploy por `APP_USERNAME` e `APP_PASSWORD`.

## Interface Web com Thymeleaf

A interface browser-facing usa templates Thymeleaf em `src/main/resources/templates`.

O projeto utiliza recursos reais do Thymeleaf, incluindo:

- `th:text`
- `th:each`
- `th:object`
- `th:field`
- `th:action`
- `th:if`
- `th:errors`

As telas cobertas pela interface sao:

- home publica
- login
- listagem de produtos
- formulario de criacao
- detalhes do produto
- formulario de edicao
- pagina amigavel de produto nao encontrado

## CRUD

Todas as operacoes do CRUD usam a tabela `TDS_MVC_TB_MERCADO` por meio do `MercadoService` e `MercadoRepository`.

### Create

O usuario acessa **Novo Produto**, preenche o formulario e envia `POST /mercado`.

### Read

A listagem aparece em `GET /mercado`, e os detalhes de um item aparecem em `GET /mercado/{id}`.

### Update

O usuario acessa **Editar**, altera os campos e envia `PUT /mercado/{id}` usando o filtro de metodo oculto do Spring MVC.

### Delete

O usuario aciona **Excluir**, e a remocao e enviada como `DELETE /mercado/{id}`. O projeto nao usa GET para excluir registros.

## Rotas MVC

| Metodo | Rota | Funcao |
| ------ | ---- | ------ |
| GET | `/` | Home publica |
| GET | `/login` | Pagina de login |
| GET | `/mercado` | Listar produtos |
| GET | `/mercado/novo` | Formulario de criacao |
| POST | `/mercado` | Criar produto |
| GET | `/mercado/{id}` | Detalhes do produto |
| GET | `/mercado/{id}/editar` | Formulario de edicao |
| PUT | `/mercado/{id}` | Atualizar produto |
| DELETE | `/mercado/{id}` | Excluir produto |

## API REST adicional

A API REST fica em `/api/mercado` e usa DTOs especificos, sem expor diretamente a entidade JPA.

| Metodo | Rota | Funcao |
| ------ | ---- | ------ |
| GET | `/api/mercado` | Listar produtos |
| GET | `/api/mercado/{id}` | Buscar produto por ID |
| POST | `/api/mercado` | Criar produto |
| PUT | `/api/mercado/{id}` | Atualizar produto completo |
| PATCH | `/api/mercado/{id}` | Atualizar campos parcialmente |
| DELETE | `/api/mercado/{id}` | Excluir produto |

O `PATCH` implementa atualizacao parcial verdadeira. Por exemplo, enviar apenas:

```json
{
  "preco": 6.49
}
```

altera somente o campo `preco`.

## HATEOAS

As respostas REST usam Spring HATEOAS com `RepresentationModelAssembler`, `EntityModel`, `CollectionModel`, `linkTo` e `methodOn`.

Exemplo simplificado de recurso:

```json
{
  "id": 1,
  "nome": "Cafe especial",
  "tipo": "Bebida",
  "setor": "Mercearia",
  "tamanho": "500 g",
  "preco": 19.90,
  "_links": {
    "self": {
      "href": "http://host/api/mercado/1"
    },
    "collection": {
      "href": "http://host/api/mercado"
    }
  }
}
```

Links:

- `self`: recurso atual
- `collection`: colecao completa de produtos

## Validacao e tratamento de erros

A entidade e os DTOs validam os campos obrigatorios, tamanho maximo e preco.

Na interface MVC, erros de formulario sao exibidos no proprio template com `th:errors`.

Na API REST, erros de validacao retornam `400 Bad Request` em JSON, e produtos inexistentes retornam `404 Not Found` em JSON. A pagina MVC continua usando a tela amigavel `mercado/nao-encontrado.html`.

## Configuracao do Spring Initializr

O projeto foi criado com as dependencias necessarias para MVC, Thymeleaf, JPA, Oracle, Security, Validation e Lombok.

<p align="center">
  <img src="docs/images/spring-initializr.png" width="900" alt="Configuracao final do Spring Initializr">
</p>

## Screenshots

A imagem real disponivel no repositorio atualmente e:

- `docs/images/spring-initializr.png`

Screenshots das telas de CRUD podem ser adicionados posteriormente em `docs/images/`, sem alterar a implementacao.

## Como executar localmente

Clone o repositorio:

```powershell
git clone git@github.com:GusCrevelari/mercado-api-II.git
cd mercado-api-II
```

Configure as variaveis de ambiente:

```powershell
$env:DB_URL="jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL"
$env:DB_USERNAME="seu_usuario_oracle"
$env:DB_PASSWORD="sua_senha_oracle"
$env:APP_USERNAME="admin"
$env:APP_PASSWORD="sua_senha_da_aplicacao"
```

Execute:

```powershell
.\mvnw.cmd spring-boot:run
```

Acesse:

```text
http://localhost:8082
```

## Deploy

O projeto esta preparado para deploy no Render usando Docker.

Variaveis de ambiente necessarias no Render:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
APP_USERNAME
APP_PASSWORD
```

O Dockerfile usa Java 17, build multi-stage e respeita a porta injetada pelo Render via variavel `PORT`, pois a aplicacao esta configurada com:

```properties
server.port=${PORT:8082}
```

O endereco de producao sera adicionado apos a publicacao no Render.

## Estrutura do projeto

```text
.
├── Dockerfile
├── README.md
├── docs
│   ├── database
│   │   └── oracle-schema.sql
│   ├── images
│   │   └── spring-initializr.png
│   └── video-checklist.md
├── pom.xml
└── src
    └── main
        ├── java/br/com/fiap/mercadomvc
        │   ├── api
        │   ├── config
        │   ├── controller
        │   ├── exception
        │   ├── model
        │   ├── repository
        │   └── service
        └── resources
            ├── application.properties
            ├── static/css/style.css
            └── templates
```
