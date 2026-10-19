# Agenda de Docas

Sistema para o centro de distribuição agendar a descarga dos caminhões dos fornecedores: em qual doca, em qual dia e em qual horário cada carga vai chegar.

Projeto Final da disciplina de Backend (Engenharia de Software, SATC). **Etapa 1: arquitetura do backend.**

## Índice

- [Integrantes](#integrantes)
- [O problema](#o-problema)
- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [Entidades](#entidades)
- [Rotas](#rotas)
- [Como executar](#como-executar)
- [Situação do projeto](#situação-do-projeto)
- [Documentação](#documentação)

## Integrantes

| Nome | GitHub |
|---|---|
| Henrique Lucio Coral | [@henriquecoral06](https://github.com/henriquecoral06) |
| Gabriel Dalmolin | [@dalmolin-gabriel](https://github.com/dalmolin-gabriel) |
| Jhonatan Inácio | [@jhonataninacio6](https://github.com/jhonataninacio6) |

## O problema

**Tema sorteado:** Logística e Cadeia de Suprimentos

Num centro de distribuição, os caminhões dos fornecedores chegam sem hora marcada e o agendamento é feito por WhatsApp e numa planilha. Dois caminhões acabam marcados para a mesma doca no mesmo horário e ficam na fila do pátio; carga refrigerada é mandada para uma doca sem câmara fria e espera no sol; e fornecedor que avisa em cima da hora desorganiza a equipe de descarga do dia. O sistema guarda as docas, os fornecedores e cada agendamento, e só aceita um agendamento que a doca consegue receber.

**Quem usa o sistema:**

| Quem | O que faz no sistema |
|---|---|
| Coordenador do centro de distribuição | Cadastra as docas e os fornecedores |
| Fornecedor | Agenda a descarga da sua carga numa doca |

**Regras de negócio.** O grupo criou estas três regras olhando para o problema da planilha: hoje nada impede dois caminhões na mesma doca, carga refrigerada em doca comum, nem agendamento em cima da hora. As regras ficam todas no Service, nenhuma no banco.

| # | Regra | Onde está no código | Rota que usa |
|---|---|---|---|
| R1 | Uma doca não pode ter dois agendamentos com horários que se sobrepõem | `AgendamentoService.criar` | `POST /agendamentos` |
| R2 | Carga refrigerada só pode ser agendada em doca refrigerada | `AgendamentoService.criar` | `POST /agendamentos` |
| R3 | O agendamento precisa ser feito com no mínimo 24 horas de antecedência | `AgendamentoService.criar` | `POST /agendamentos` |

Quando uma regra barra o pedido, a API devolve `400` com a regra explicada na mensagem.

**Fora do escopo:** cancelamento e remarcação de agendamentos, registro de chegada e saída do caminhão no pátio, login de usuários e tela de frontend.

## Tecnologias

| O quê | Qual |
|---|---|
| Linguagem | Java 25 |
| Framework | Spring Boot 3.5 (Web, Validation, Data JPA) |
| Banco de dados | PostgreSQL 16, em Docker |
| Migrations | Flyway |
| Documentação da API | springdoc-openapi (Swagger UI) |

## Arquitetura

A API é organizada em camadas. Uma requisição entra pelo Controller, passa pelo Service, que aplica as regras de negócio, e chega ao banco pelo Repository; a resposta faz o caminho de volta. O Mapper converte DTO em entidade e entidade em DTO.

**Mapa da estrutura:**

| Entidade | Migration | Model | Repository | DTOs | Mapper | Service | Controller |
|---|---|---|---|---|---|---|---|
| Doca | `V1__criar_docas.sql`, pronta | `Doca`, pronta | `DocaRepository`, pronto | `DocaRequestDTO`, `DocaResponseDTO`, prontos | `DocaMapper`, implementado | `DocaService`, implementado | `DocaController`, implementado |
| Fornecedor | `V2__criar_fornecedores.sql`, pronta | `Fornecedor`, pronta | `FornecedorRepository`, pronto | `FornecedorRequestDTO`, `FornecedorResponseDTO`, prontos | `FornecedorMapper`, implementado | `FornecedorService`, em parte: só o cadastro | `FornecedorController`, em parte: só o `POST` |
| Agendamento | `V3__criar_agendamentos.sql`, pronta | `Agendamento`, pronta | `AgendamentoRepository`, pronto | `AgendamentoRequestDTO`, `AgendamentoResponseDTO`, prontos | `AgendamentoMapper`, implementado | `AgendamentoService`, em parte: o `POST`, com R1, R2 e R3 | `AgendamentoController`, em parte: só o `POST` |

Também fazem parte do projeto, vindas do template: `ApiExceptionHandler`, `ErroDTO`, `CampoErroDTO`, `RecursoNaoEncontradoException`, `RegraDeNegocioException` e `OpenApiConfig`. Cada entidade usada pelas rotas tem a sua exceção de "não encontrado": `DocaNaoEncontradaException` e `FornecedorNaoEncontradoException`.

Do fornecedor, só o cadastro foi feito, porque é o que as regras precisam: sem fornecedor cadastrado, não há agendamento. As consultas de fornecedores e de agendamentos ficam para a próxima etapa.

## Entidades

| Entidade | O que representa | Como se relaciona |
|---|---|---|
| Doca | Um portão do centro de distribuição onde o caminhão descarrega | Recebe vários agendamentos, em horários diferentes |
| Fornecedor | Uma empresa que entrega mercadoria no centro de distribuição | Faz vários agendamentos |
| Agendamento | A reserva de uma doca, num horário, para a descarga de um fornecedor | Pertence a um fornecedor e a uma doca |

Atributos, tipos e o diagrama das tabelas estão em [docs/modelo-de-dominio.md](docs/modelo-de-dominio.md).

## Rotas

### As que já funcionam

| Verbo | Caminho | O que faz | Regra | Sucesso | Erros |
|---|---|---|---|---|---|
| `POST` | `/docas` | Cadastra uma doca | Nenhuma | `201` | `400` |
| `GET` | `/docas` | Lista as docas | Nenhuma | `200` | Nenhum |
| `GET` | `/docas/{id}` | Busca uma doca pelo id | Nenhuma | `200` | `404` |
| `POST` | `/fornecedores` | Cadastra um fornecedor | Nenhuma | `201` | `400` |
| `POST` | `/agendamentos` | Agenda a descarga de um fornecedor numa doca | R1, R2 e R3 | `201` | `400`, `404` |

Exemplo de requisição:

```http
POST /agendamentos
Content-Type: application/json

{
  "fornecedorId": 1,
  "docaId": 1,
  "inicio": "2026-10-27T08:00:00",
  "fim": "2026-10-27T09:00:00",
  "cargaRefrigerada": true
}
```

Resposta, `201 Created`. Entram os ids; fornecedor e doca voltam completos:

```json
{
  "id": 1,
  "fornecedor": { "id": 1, "nome": "Laticínios Serra Azul", "cnpj": "12.345.678/0001-90" },
  "doca": { "id": 1, "identificacao": "D-01", "refrigerada": true },
  "inicio": "2026-10-27T08:00:00",
  "fim": "2026-10-27T09:00:00",
  "cargaRefrigerada": true
}
```

### Erros

Todo erro sai no mesmo formato. Este é o `400` da regra R2, quando alguém tenta agendar carga refrigerada numa doca comum:

```json
{
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "R2: a carga é refrigerada e a doca D-02 não é refrigerada",
  "caminho": "/agendamentos",
  "timestamp": "2026-10-20T19:30:00",
  "campos": []
}
```

Um campo inválido também devolve `400`, com a lista `campos` dizendo qual campo corrigir. Uma doca ou um fornecedor que não existe devolve `404`.

### As planejadas

Estão no contrato, mas ainda não respondem.

| Recurso | O que vai fazer |
|---|---|
| `GET /fornecedores` e `GET /fornecedores/{id}` | Listar e buscar fornecedores |
| `GET /agendamentos` e `GET /agendamentos/{id}` | Listar e buscar agendamentos |

O contrato completo, com todas as rotas e os DTOs, está em [docs/contrato-da-api.md](docs/contrato-da-api.md).

## Como executar

Pré-requisitos: JDK 25, Docker Desktop aberto e Git.

```bash
git clone https://github.com/henriquecoral06/trabalho-01-backend.git
cd trabalho-01-backend

docker compose up -d          # sobe o PostgreSQL na porta 5437
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run
```

Com a API no ar:

| Endereço | O que é |
|---|---|
| <http://localhost:8080/swagger-ui.html> | Swagger UI: as rotas, com **Try it out** para enviar requisições |
| <http://localhost:8080/v3/api-docs> | O contrato OpenAPI em JSON |

Para testar as rotas, use o Swagger UI ou o **Postman**. No Postman, importe todas as rotas de uma vez: **Import** → **Link** → `http://localhost:8080/v3/api-docs`.

Para ver as regras funcionando: cadastre uma doca refrigerada (`D-01`), uma comum (`D-02`) e um fornecedor. Agende uma descarga na `D-01` para daqui a alguns dias e tente agendar outra na mesma doca, num horário que se sobrepõe (R1). Tente agendar carga refrigerada na `D-02` (R2). Tente agendar para daqui a poucas horas (R3).

Para parar: `Ctrl+C` na API e `docker compose stop` no banco. Para apagar o banco e rodar as migrations do zero: `docker compose down -v` e suba de novo.

## Situação do projeto

| Parte | Situação |
|---|---|
| Desenho | Completo: problema e regras neste README, modelo e contrato em `docs/` |
| Estrutura | As 3 entidades têm migration, entidade, Repository, DTOs, Mapper, Service e Controller |
| API e regras | Docas com as 3 rotas da API simples; cadastro de fornecedores; agendamento com R1, R2 e R3 implementadas |

**Ainda não funciona:** `GET /fornecedores`, `GET /fornecedores/{id}`, `GET /agendamentos` e `GET /agendamentos/{id}`.

## Documentação

| Documento | O que tem |
|---|---|
| [Modelo de domínio](docs/modelo-de-dominio.md) | Entidades, relacionamentos, diagrama das tabelas, migrations |
| [Contrato da API](docs/contrato-da-api.md) | Todas as rotas, os DTOs, exemplos de JSON e erros |
| [Autoavaliação](docs/autoavaliacao.md) | Os critérios da revisão do código, marcados pelo grupo |
