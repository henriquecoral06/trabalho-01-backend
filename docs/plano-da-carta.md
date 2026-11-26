# Entrega 01 — Plano da Carta

Projeto: **Agenda de Docas** (tema: Logística e Cadeia de Suprimentos).

## Carta sorteada

**C2 — 🔬 Caixa de Vidro** (Aula 13: Observabilidade e Testes)

> "Como responsável pela operação, quero enxergar o que a aplicação está fazendo e confiar que ela não quebrou sem eu perceber."

Critérios de aceite:

1. Operações críticas (criar, atualizar, excluir) geram log com nível apropriado (INFO/WARN/ERROR).
2. Existe suíte de testes (JUnit) cobrindo um Service, com casos de sucesso e de erro.
3. Os testes rodam via `./mvnw test` sem intervenção manual.

## Como a carta entra no domínio

O coordenador do centro de distribuição precisa saber, sem abrir o banco, **por que** um agendamento foi recusado e **quem** mexeu na agenda de uma doca. Por isso:

| O que acontece no sistema | Nível do log | Exemplo |
|---|---|---|
| Doca, fornecedor ou agendamento criado, alterado ou excluído | `INFO` | `Agendamento 7 criado: fornecedor 2, doca D-01, 2026-12-01T08:00 a 09:00` |
| Regra R1, R2, R3 ou R4 barrou o pedido | `WARN` | `Agendamento recusado pela R2: carga refrigerada na doca D-02` |
| Doca, fornecedor ou agendamento não encontrado | `WARN` | `Doca não encontrada: 99` |
| Erro inesperado (banco fora do ar, bug) | `ERROR` | com a stack trace, e o cliente recebe `500` sem detalhe interno |

Cada linha de log carrega o **id da requisição** (`X-Request-Id`), como na Aula 13, para seguir um agendamento recusado do começo ao fim. O log também sai em **JSON (formato ECS)** num arquivo, com os campos (`operacao`, `docaId`, `regra`...) separados — é o log estruturado que uma ferramenta como o Grafana/Loki ou o Kibana lê.

Além dos logs, o Actuator expõe a saúde da API e as métricas em `/actuator/prometheus`, com um contador por operação e um contador **por regra de negócio violada** (`agenda_regras_violadas_total{regra="R1"}`): o coordenador vê qual regra mais barra fornecedores.

## Entidades e rotas afetadas

| Entidade | O que muda |
|---|---|
| `Doca` | CRUD completo, listagem paginada com filtros, `@OneToMany` para os agendamentos, log em criar/alterar/excluir |
| `Fornecedor` | CRUD completo, listagem paginada com filtros, `@OneToMany` para os agendamentos, log em criar/alterar/excluir |
| `Agendamento` | CRUD completo (alterar reaplica R1, R2 e R3), listagem paginada com filtros, log de cada regra que recusa um pedido |
| Aplicação prática (📈 Visualização de Dados) | `GET /docas/ocupacao`: taxa de ocupação de cada doca num período, a partir dos agendamentos |

Rotas novas: `PUT` e `DELETE` nas três entidades, `GET /fornecedores`, `GET /fornecedores/{id}`, `GET /agendamentos`, `GET /agendamentos/{id}`, `GET /docas/ocupacao` e os endpoints do Actuator (`/actuator/health`, `/actuator/prometheus`).

## Dependências novas no `pom.xml`

| Dependência | Para quê |
|---|---|
| `spring-boot-starter-actuator` | `/actuator/health` e `/actuator/metrics` (Aula 13) |
| `micrometer-registry-prometheus` | Métricas em `/actuator/prometheus` |
| `spring-boot-starter-test` (escopo `test`) | JUnit 5, Mockito, AssertJ e MockMvc |
| `maven-surefire-plugin` 3.x | Roda o JUnit 5 no `./mvnw test` |

O log estruturado em JSON não precisa de dependência nova: o Spring Boot 3.5 já traz o suporte (`logging.structured.format.file=ecs`).

## Maior dúvida ou risco

- **Testes que dependem do banco.** Se a suíte precisar do PostgreSQL ligado, o `./mvnw test` deixa de rodar "sem intervenção manual". Decisão: os testes dos Services usam **Mockito** no lugar dos Repositories, e os dos Controllers usam `@WebMvcTest`; nenhum precisa de banco nem de Docker.
- **Testar a regra R3 sem depender do relógio.** A R3 compara com "agora": os testes montam os horários a partir de `LocalDateTime.now()` (daqui a 3 dias passa, daqui a 2 horas é barrado), então não quebram com a data em que forem rodados.
