# Agenda de Docas

Sistema para o centro de distribuição agendar a descarga dos caminhões dos fornecedores: em qual doca, em qual dia e em qual horário cada carga vai chegar — e enxergar quanto cada doca está ocupada.

Projeto Final (N3) da disciplina de Backend (Engenharia de Software, SATC 2026.2). Evolução da Etapa 1, no mesmo repositório. **Carta-desafio sorteada: C2 — 🔬 Caixa de Vidro** (Observabilidade e Testes).

## Índice

1. [Projeto e integrantes](#1-projeto-e-integrantes)
2. [O problema](#2-o-problema)
3. [Tecnologias](#3-tecnologias)
4. [Limitações](#4-limitações)
5. [Entidades](#5-entidades)
6. [Rotas](#6-rotas)
7. [Erros HTTP](#7-erros-http)
8. [Como executar](#8-como-executar)
9. [Carta-desafio: C2 — Caixa de Vidro](#9-carta-desafio-c2--caixa-de-vidro)
10. [Documentação complementar](#10-documentação-complementar)

## 1. Projeto e integrantes

API REST em Spring Boot que guarda as docas, os fornecedores e os agendamentos de descarga de um centro de distribuição, aplica as regras de negócio do agendamento, calcula a ocupação das docas para um dashboard e deixa visível — por logs estruturados, métricas e uma suíte de testes — tudo o que acontece com a agenda.

| Nome | GitHub |
|---|---|
| Henrique Lucio Coral | [@henriquecoral06](https://github.com/henriquecoral06) |
| Gabriel Dalmolin | [@dalmolin-gabriel](https://github.com/dalmolin-gabriel) |
| Jhonatan Inácio | [@jhonataninacio6](https://github.com/jhonataninacio6) |

## 2. O problema

**Tema sorteado:** Logística e Cadeia de Suprimentos.

Num centro de distribuição, os caminhões dos fornecedores chegam sem hora marcada e o agendamento é feito por WhatsApp e numa planilha. Dois caminhões acabam marcados para a mesma doca no mesmo horário e ficam na fila do pátio; carga refrigerada é mandada para uma doca sem câmara fria e espera no sol; e fornecedor que avisa em cima da hora desorganiza a equipe de descarga do dia. O sistema guarda as docas, os fornecedores e cada agendamento, e só aceita um agendamento que a doca consegue receber.

| Quem usa | O que faz no sistema |
|---|---|
| Coordenador do centro de distribuição | Cadastra docas e fornecedores, acompanha a ocupação das docas e os logs |
| Fornecedor | Agenda, remarca e cancela a descarga da sua carga |

**Regras de negócio.** Todas ficam nos Services; nenhuma no banco.

| # | Regra | Onde está no código | Rotas que usam |
|---|---|---|---|
| R1 | Uma doca não pode ter dois agendamentos com horários que se sobrepõem | `AgendamentoService.validarRegras` | `POST` e `PUT /agendamentos` |
| R2 | Carga refrigerada só pode ser agendada em doca refrigerada | `AgendamentoService.validarRegras` | `POST` e `PUT /agendamentos` |
| R3 | O agendamento precisa ser feito com no mínimo 24 horas de antecedência | `AgendamentoService.validarRegras` | `POST` e `PUT /agendamentos` |
| R4 | Doca ou fornecedor que tem agendamentos não pode ser excluído | `DocaService.excluir`, `FornecedorService.excluir` | `DELETE /docas/{id}`, `DELETE /fornecedores/{id}` |

Quando uma regra barra o pedido, a API devolve `400` com a regra explicada na mensagem — e registra um log `WARN` com o código da regra (veja a [seção 9](#9-carta-desafio-c2--caixa-de-vidro)).

**Aplicação prática escolhida: 📈 Visualização de Dados.** A rota `GET /docas/ocupacao?de=&ate=` calcula, a partir dos agendamentos que já existem, quanto cada doca fica ocupada num período: horas reservadas, taxa de ocupação, a taxa geral do CD e **quantas horas de câmara fria foram gastas com carga seca**. Câmara fria é o recurso mais disputado do CD: cada hora de doca refrigerada ocupada por carga seca é uma hora que uma carga refrigerada não consegue agendar (a R2 a barraria nas docas comuns). A resposta já vem com alertas em português para o coordenador ("a doca refrigerada D-01 tem 4.0 h ocupadas por carga seca: essas cargas podem ir para uma doca comum"; "a doca D-02 está 91.7% ocupada: perto do limite"). Nenhuma entidade nova foi criada: o cálculo usa `Doca` e `Agendamento`.

## 3. Tecnologias

| O quê | Qual |
|---|---|
| Linguagem | Java 25 |
| Framework | Spring Boot 3.5 (Web, Validation, Data JPA) |
| Banco de dados | PostgreSQL 16, em Docker (`docker-compose.yml`) |
| Migrations | Flyway |
| Documentação da API | springdoc-openapi 2.9 (Swagger UI) |
| **Carta C2:** observabilidade | Spring Boot Actuator, Micrometer + `micrometer-registry-prometheus`, SLF4J/Logback com log estruturado ECS (JSON) nativo do Spring Boot 3.5, MDC com id de requisição |
| **Carta C2:** testes | JUnit 5, Mockito, AssertJ, `@WebMvcTest`/MockMvc (`spring-boot-starter-test`), Maven Surefire 3.5 |

## 4. Limitações

- **Sem autenticação.** Qualquer cliente pode criar, alterar e excluir (a carta C4 não foi a sorteada).
- **Exclusão física.** Cancelar um agendamento apaga a linha; não fica histórico de cancelamentos (só a linha `INFO` no log).
- **R3 usa o relógio do servidor**, sem fuso horário por fornecedor.
- **Ocupação considera a doca disponível 24 h por dia**; não há cadastro de turnos nem de janela de funcionamento por doca.
- **Logs e métricas ficam na própria API**: o log JSON vai para `logs/agenda-docas.log` e as métricas para `/actuator/prometheus`, mas o projeto não sobe Prometheus/Grafana/Loki para guardá-los e desenhar painéis.
- **Testes sem banco real.** A suíte usa Mockito no lugar dos Repositories e `@WebMvcTest` para a camada web; as consultas do Spring Data (R1, filtros) foram conferidas manualmente contra o PostgreSQL, mas não há teste automatizado com Testcontainers.
- **Sem frontend**: a API é demonstrada pelo Swagger UI, Postman ou curl.

## 5. Entidades

| Entidade | O que representa | Atributos | Relacionamentos |
|---|---|---|---|
| `Doca` | Um portão do CD onde o caminhão descarrega | `id`, `identificacao` (ex.: `D-01`, até 20 caracteres), `refrigerada` | `@OneToMany(mappedBy = "doca")` → vários `Agendamento` |
| `Fornecedor` | Empresa que entrega mercadoria no CD | `id`, `nome`, `cnpj` (até 18 caracteres) | `@OneToMany(mappedBy = "fornecedor")` → vários `Agendamento` |
| `Agendamento` | A reserva de uma doca, num horário, para a descarga de um fornecedor | `id`, `inicio`, `fim`, `cargaRefrigerada` | `@ManyToOne` → um `Fornecedor` e uma `Doca` |

Camadas, por entidade: `Controller → Service → Repository`, com DTOs de entrada e saída (nenhuma rota expõe a `@Entity`) e um Mapper convertendo entre os dois. Atributos, tipos, colunas e o diagrama ER estão em [docs/modelo-de-dominio.md](docs/modelo-de-dominio.md).

| Entidade | Controller | Service (métodos CRUD) | Repository | DTOs |
|---|---|---|---|---|
| Doca | `DocaController` | `DocaService`: `criar`, `listar`, `buscarPorId`, `atualizar`, `excluir` | `DocaRepository` | `DocaRequestDTO`, `DocaResponseDTO` |
| Fornecedor | `FornecedorController` | `FornecedorService`: `criar`, `listar`, `buscarPorId`, `atualizar`, `excluir` | `FornecedorRepository` | `FornecedorRequestDTO`, `FornecedorResponseDTO` |
| Agendamento | `AgendamentoController` | `AgendamentoService`: `criar`, `listar`, `buscarPorId`, `atualizar`, `excluir` | `AgendamentoRepository` | `AgendamentoRequestDTO`, `AgendamentoResponseDTO` |
| (ocupação) | `DocaController` | `OcupacaoService`: `calcular` | `DocaRepository`, `AgendamentoRepository` | `OcupacaoDTO`, `OcupacaoDocaDTO` |

## 6. Rotas

Endereço base: `http://localhost:8080`. Datas e horas no formato `yyyy-MM-ddTHH:mm:ss`; filtros de dia no formato `yyyy-MM-dd`.

| Verbo | Caminho | O que faz | Regra | Sucesso | Erros |
|---|---|---|---|---|---|
| `POST` | `/docas` | Cadastra uma doca | — | `201` | `400` |
| `GET` | `/docas` | Lista as docas (paginado; filtros `identificacao`, `refrigerada`) | — | `200` | `400` |
| `GET` | `/docas/{id}` | Busca uma doca | — | `200` | `404` |
| `PUT` | `/docas/{id}` | Altera uma doca | — | `200` | `400`, `404` |
| `DELETE` | `/docas/{id}` | Exclui uma doca | R4 | `204` | `400`, `404` |
| `GET` | `/docas/ocupacao?de=&ate=` | Ocupação das docas no período (até 31 dias) | — | `200` | `400` |
| `POST` | `/fornecedores` | Cadastra um fornecedor | — | `201` | `400` |
| `GET` | `/fornecedores` | Lista os fornecedores (paginado; filtros `nome`, `cnpj`) | — | `200` | `400` |
| `GET` | `/fornecedores/{id}` | Busca um fornecedor | — | `200` | `404` |
| `PUT` | `/fornecedores/{id}` | Altera um fornecedor | — | `200` | `400`, `404` |
| `DELETE` | `/fornecedores/{id}` | Exclui um fornecedor | R4 | `204` | `400`, `404` |
| `POST` | `/agendamentos` | Agenda uma descarga | R1, R2, R3 | `201` | `400`, `404` |
| `GET` | `/agendamentos` | Lista os agendamentos (paginado; filtros `docaId`, `fornecedorId`, `cargaRefrigerada`, `de`, `ate`) | — | `200` | `400` |
| `GET` | `/agendamentos/{id}` | Busca um agendamento | — | `200` | `404` |
| `PUT` | `/agendamentos/{id}` | Remarca um agendamento | R1, R2, R3 | `200` | `400`, `404` |
| `DELETE` | `/agendamentos/{id}` | Cancela um agendamento | — | `204` | `404` |
| `GET` | `/actuator/health` | Saúde da API e do banco (carta C2) | — | `200` | — |
| `GET` | `/actuator/prometheus` | Métricas no formato Prometheus (carta C2) | — | `200` | — |

**Paginação e ordenação** em todo `GET` de listagem: `?page=0&size=10&sort=campo,asc`. Padrões: docas por `identificacao`, fornecedores por `nome`, agendamentos por `inicio`. Todos os filtros são opcionais e podem ser combinados.

### Exemplos

**`POST /docas`**

```json
{ "identificacao": "D-01", "refrigerada": true }
```

`201 Created`:

```json
{ "id": 1, "identificacao": "D-01", "refrigerada": true }
```

**`GET /docas?refrigerada=true&identificacao=d-0&page=0&size=10`** → `200 OK`:

```json
{
  "content": [ { "id": 1, "identificacao": "D-01", "refrigerada": true } ],
  "page": { "size": 10, "number": 0, "totalElements": 1, "totalPages": 1 }
}
```

**`PUT /fornecedores/1`**

```json
{ "nome": "Laticínios Serra Azul Ltda", "cnpj": "12.345.678/0001-90" }
```

`200 OK`:

```json
{ "id": 1, "nome": "Laticínios Serra Azul Ltda", "cnpj": "12.345.678/0001-90" }
```

**`POST /agendamentos`** (entram os ids; fornecedor e doca voltam completos)

```json
{
  "fornecedorId": 1,
  "docaId": 1,
  "inicio": "2026-12-10T08:00:00",
  "fim": "2026-12-10T10:00:00",
  "cargaRefrigerada": true
}
```

`201 Created`:

```json
{
  "id": 1,
  "fornecedor": { "id": 1, "nome": "Laticínios Serra Azul", "cnpj": "12.345.678/0001-90" },
  "doca": { "id": 1, "identificacao": "D-01", "refrigerada": true },
  "inicio": "2026-12-10T08:00:00",
  "fim": "2026-12-10T10:00:00",
  "cargaRefrigerada": true
}
```

**`PUT /agendamentos/1`**: mesmo corpo do `POST`, com os dados novos; devolve `200` com o agendamento remarcado. As regras R1, R2 e R3 valem de novo (na R1, o próprio agendamento não conta como conflito).

**`GET /agendamentos?docaId=1&de=2026-12-10&ate=2026-12-12&sort=inicio,desc`** → `200 OK`, mesma estrutura de página do `GET /docas`.

**`DELETE /agendamentos/1`** → `204 No Content`, sem corpo.

**`GET /docas/ocupacao?de=2026-12-10&ate=2026-12-10`** → `200 OK`:

```json
{
  "de": "2026-12-10",
  "ate": "2026-12-10",
  "horasDisponiveisPorDoca": 24.0,
  "taxaOcupacaoGeral": 12.5,
  "docas": [
    {
      "docaId": 1, "identificacao": "D-01", "refrigerada": true,
      "agendamentos": 2, "horasAgendadas": 6.0, "taxaOcupacao": 25.0,
      "horasDeCargaSecaEmDocaRefrigerada": 4.0
    },
    {
      "docaId": 2, "identificacao": "D-02", "refrigerada": false,
      "agendamentos": 0, "horasAgendadas": 0.0, "taxaOcupacao": 0.0,
      "horasDeCargaSecaEmDocaRefrigerada": 0.0
    }
  ],
  "alertas": [
    "A doca refrigerada D-01 tem 4.0 h ocupadas por carga seca: essas cargas podem ir para uma doca comum"
  ]
}
```

Só a parte do agendamento que cai dentro do período conta: um agendamento das 22h às 02h do dia seguinte soma 2 h no primeiro dia.

O contrato completo, com todos os DTOs, está em [docs/contrato-da-api.md](docs/contrato-da-api.md) e, vivo, no Swagger UI.

## 7. Erros HTTP

Todo erro sai no mesmo formato, `ErroDTO`. A resposta também traz o cabeçalho `X-Request-Id`, o mesmo id que aparece em cada linha de log daquela requisição.

**`400` — regra de negócio** (`POST /agendamentos` com carga refrigerada numa doca comum):

```json
{
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "R2: a carga é refrigerada e a doca D-02 não é refrigerada",
  "caminho": "/agendamentos",
  "timestamp": "2026-12-01T19:30:00",
  "campos": []
}
```

**`400` — validação** (`POST /docas` com corpo `{}`): a lista `campos` diz o que corrigir.

```json
{
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Dados inválidos: confira a lista de campos",
  "caminho": "/docas",
  "timestamp": "2026-12-01T19:30:00",
  "campos": [
    { "campo": "identificacao", "mensagem": "Identificação é obrigatória" },
    { "campo": "refrigerada", "mensagem": "Informe se a doca é refrigerada" }
  ]
}
```

**`404` — não encontrado** (`GET /docas/99`):

```json
{
  "status": 404,
  "erro": "Not Found",
  "mensagem": "Doca não encontrada: 99",
  "caminho": "/docas/99",
  "timestamp": "2026-12-01T19:30:00",
  "campos": []
}
```

**`500` — erro inesperado** (ex.: banco fora do ar). O cliente não recebe detalhe interno; a stack trace vai para o log `ERROR`, com o mesmo `X-Request-Id`:

```json
{
  "status": 500,
  "erro": "Internal Server Error",
  "mensagem": "Erro interno: a falha foi registrada no log da API com o id desta requisição",
  "caminho": "/docas",
  "timestamp": "2026-12-01T19:30:00",
  "campos": []
}
```

| Status | Quando acontece |
|---|---|
| `400` | JSON ilegível; campo inválido no DTO; parâmetro em formato errado (`/docas/abc`, `?de=01/12/2026`); parâmetro obrigatório ausente; `sort` por campo inexistente; regra R1, R2, R3 ou R4 violada; `fim` que não vem depois do `inicio`; período da ocupação invertido ou maior que 31 dias |
| `404` | Id de doca, fornecedor ou agendamento que não existe; rota inexistente |
| `405` | Verbo não suportado na rota (ex.: `PATCH /docas/1`) |
| `500` | Falha inesperada da API (registrada em `ERROR`) |

## 8. Como executar

Pré-requisitos: **JDK 25**, **Docker** (Docker Desktop aberto) e Git.

```bash
git clone https://github.com/henriquecoral06/trabalho-01-backend.git
```

```bash
cd trabalho-01-backend
```

Suba o PostgreSQL (porta 5437; usuário, senha e banco `docas`, os mesmos de `src/main/resources/application.properties`):

```bash
docker compose up -d
```

Rode a API (Windows: `.\mvnw.cmd spring-boot:run`). O Flyway cria as tabelas na subida:

```bash
./mvnw spring-boot:run
```

Rode a suíte de testes (não precisa do banco nem do Docker):

```bash
./mvnw test
```

Com a API no ar:

| Endereço | O que é |
|---|---|
| <http://localhost:8080/swagger-ui.html> | Swagger UI, com **Try it out** em todas as rotas |
| <http://localhost:8080/v3/api-docs> | Contrato OpenAPI (no Postman: **Import** → **Link**) |
| <http://localhost:8080/actuator/health> | Saúde da API e do banco |
| <http://localhost:8080/actuator/prometheus> | Métricas (procure por `agenda_`) |
| `logs/agenda-docas.log` | Log estruturado em JSON (ECS), criado ao rodar a API |

Para parar: `Ctrl+C` na API e `docker compose stop` no banco. Para apagar o banco e rodar as migrations do zero: `docker compose down -v`.

## 9. Carta-desafio: C2 — Caixa de Vidro

> "Como responsável pela operação, quero enxergar o que a aplicação está fazendo e confiar que ela não quebrou sem eu perceber."

O plano entregue no início da Aula 17 está em [docs/plano-da-carta.md](docs/plano-da-carta.md). Os três critérios de aceite, e onde cada um está:

### Critério 1 — Operações críticas geram log com nível apropriado

| Situação | Nível | Onde |
|---|---|---|
| Doca, fornecedor ou agendamento criado, alterado ou excluído | `INFO` | `DocaService`, `FornecedorService`, `AgendamentoService` |
| Regra R1, R2, R3 ou R4 recusou o pedido | `WARN`, com o campo `regra` | `Operacoes.regraViolada`, chamado pelos Services |
| Recurso não encontrado, validação reprovada, parâmetro inválido, rota/verbo inexistente | `WARN` | `ApiExceptionHandler` |
| Falha inesperada (banco fora do ar, bug) | `ERROR`, com stack trace | `ApiExceptionHandler.tratarErroInesperado` |
| Toda requisição: método, rota, status e tempo | `INFO` (2xx), `WARN` (4xx), `ERROR` (5xx) | `RequisicaoIdFilter` |

**Integração com o domínio.** O log conta a história da agenda do CD, não só "deu certo/deu erro": quem agendou, em qual doca, em qual horário, e qual regra barrou o fornecedor. Uma requisição recusada pela R2, no console:

```
WARN [demo-r2] ... api.observability.Operacoes          : Pedido recusado (R2): R2: a carga é refrigerada e a doca D-03 não é refrigerada
WARN [demo-r2] ... api.observability.RequisicaoIdFilter : POST /agendamentos -> 400 (16 ms)
```

O `[demo-r2]` é o **id da requisição** (cabeçalho `X-Request-Id`, gerado pela API ou enviado pelo cliente; guardado no MDC). Quando o fornecedor diz "meu agendamento foi recusado", o coordenador procura o id que voltou na resposta e acha todas as linhas daquela requisição.

**Log estruturado.** O mesmo evento sai também em `logs/agenda-docas.log`, uma linha JSON por evento no formato ECS (Elastic Common Schema), com cada informação num campo próprio — pronto para Kibana, Grafana/Loki ou `jq`:

```json
{"@timestamp":"2026-11-26T23:00:01.307Z","log":{"level":"INFO","logger":"api.service.AgendamentoService"},
 "service":{"name":"agenda-docas"},"message":"Agendamento 3 cancelado: fornecedor Laticínios Serra Azul, doca D-01, de 2026-12-01T08:00 a 2026-12-01T09:00 (carga refrigerada: true)",
 "requisicaoId":"6a46c4c2","operacao":"excluir","agendamentoId":3,"docaId":1,"fornecedorId":1,"ecs":{"version":"8.11"}}
```

Os campos vêm da API fluente do SLF4J 2 (`log.atInfo().addKeyValue("docaId", ...)`); a configuração está no fim do `application.properties` (`logging.structured.format.file=ecs`). Por exemplo, todas as recusas da R2:

```bash
grep '"regra":"R2"' logs/agenda-docas.log
```

**Métricas (além do pedido pela carta).** O Actuator expõe `/actuator/health` (API + PostgreSQL) e `/actuator/prometheus`, com as métricas HTTP e da JVM e dois contadores do domínio:

```
agenda_operacoes_total{entidade="agendamento",operacao="criar"} 12.0
agenda_regras_violadas_total{regra="R3"} 5.0
```

Se a R3 é a regra que mais barra, os fornecedores estão avisando em cima da hora — é o problema da planilha aparecendo em número.

### Critério 2 — Suíte de testes (JUnit) cobrindo Services, com sucesso e erro

38 testes em `src/test/java`, sem banco: os Repositories são mocks do Mockito e a camada web roda com `@WebMvcTest`. Além do resultado, os testes conferem **o log e a métrica** de cada operação (com `CapturaDeLog`, um `ListAppender` do Logback) — a "caixa de vidro" também é testada.

| Classe de teste | O que cobre | Sucesso | Erro |
|---|---|---|---|
| `AgendamentoServiceTest` (14) | criar, remarcar, cancelar, buscar | grava com log `INFO` e campos estruturados; carga seca em doca fria; remarcação ignora o próprio id na R1 | R1, R2, R3 (com `WARN` e métrica da regra), fim antes do início, fornecedor/doca/agendamento inexistente, R2 na remarcação |
| `DocaServiceTest` (6) | CRUD da doca | criar, listar paginado, alterar (log com antes/depois), excluir | doca inexistente, R4 com `WARN` |
| `FornecedorServiceTest` (5) | CRUD do fornecedor | criar, alterar, excluir | fornecedor inexistente, R4 |
| `OcupacaoServiceTest` (4) | aplicação prática | horas cortadas na borda do período, carga seca em doca fria, alerta de limite | período invertido, período > 31 dias |
| `AgendamentoControllerTest` (9) | camada web + `ApiExceptionHandler` + filtro | `201`, `204`, `X-Request-Id` gerado e reaproveitado, filtros repassados | `400` de validação (com `WARN`), `400` de regra, `404` (com `WARN`), `500` sem vazar detalhe (com `ERROR` e stack trace), `405`, data inválida |

### Critério 3 — Os testes rodam via `./mvnw test` sem intervenção manual

```bash
./mvnw test
```

```
[INFO] Tests run: 38, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Não precisa de PostgreSQL, Docker, variável de ambiente nem dado cadastrado antes. A R3 é testada com horários calculados a partir de `LocalDateTime.now()`, então os testes não quebram com a data em que forem rodados.

### Roteiro para demonstrar a carta

1. `./mvnw test` → 38 testes verdes.
2. Suba a API e cadastre duas docas (`D-01` refrigerada, `D-02` comum) e um fornecedor.
3. Agende numa `D-01` daqui a 3 dias → log `INFO` "Agendamento 1 criado".
4. Envie, com o cabeçalho `X-Request-Id: demo-r2`, carga refrigerada na `D-02` → `400` e log `WARN` "Pedido recusado (R2)" com `[demo-r2]`.
5. Agende para daqui a 2 horas → `WARN` da R3. Tente excluir a `D-01` → `WARN` da R4.
6. Mostre `grep '"regra"' logs/agenda-docas.log` e, em `/actuator/prometheus`, `agenda_regras_violadas_total`.
7. Pare o banco (`docker compose stop`) e cadastre uma doca → `500` para o cliente, `ERROR` com stack trace no log, e `/actuator/health` com `"db": "DOWN"`.

## 10. Documentação complementar

| Documento | O que tem |
|---|---|
| [Plano da carta](docs/plano-da-carta.md) | Entrega 01 do N3: carta, entidades e rotas afetadas, dependências, riscos |
| [Modelo de domínio](docs/modelo-de-dominio.md) | Entidades, relacionamentos, diagrama ER, migrations |
| [Contrato da API](docs/contrato-da-api.md) | Todas as rotas, os DTOs, exemplos de JSON e erros |
| [Autoavaliação](docs/autoavaliacao.md) | Critérios da revisão do código da Etapa 1 |
