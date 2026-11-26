# Contrato da API

## Convenções

| Item | Valor |
|---|---|
| Endereço base | `http://localhost:8080` |
| Formato | JSON, na requisição e na resposta |
| Data e hora | `yyyy-MM-ddTHH:mm:ss`, por exemplo `2026-12-10T08:00:00` |
| Dia (filtros e ocupação) | `yyyy-MM-dd`, por exemplo `2026-12-10` |
| Paginação | `?page=0&size=10&sort=campo,asc` em todo `GET` de listagem |
| Erros | Sempre no formato `ErroDTO` (ver [Erros](#erros)) |
| Rastreio | Toda resposta traz o cabeçalho `X-Request-Id`; o cliente pode enviar o seu |
| Documentação viva | `http://localhost:8080/swagger-ui.html` |

## Rotas

### Docas

| Verbo | Caminho | O que faz | Regra | Entrada | Saída | Sucesso | Erros |
|---|---|---|---|---|---|---|---|
| `POST` | `/docas` | Cadastra uma doca | — | `DocaRequestDTO` | `DocaResponseDTO` | `201` | `400` |
| `GET` | `/docas` | Lista as docas | — | filtros `identificacao` (contém), `refrigerada` | página de `DocaResponseDTO` | `200` | `400` |
| `GET` | `/docas/{id}` | Busca uma doca | — | — | `DocaResponseDTO` | `200` | `404` |
| `PUT` | `/docas/{id}` | Altera uma doca | — | `DocaRequestDTO` | `DocaResponseDTO` | `200` | `400`, `404` |
| `DELETE` | `/docas/{id}` | Exclui uma doca | R4 | — | — | `204` | `400`, `404` |
| `GET` | `/docas/ocupacao` | Ocupação das docas no período | período ≤ 31 dias | `de`, `ate` (obrigatórios) | `OcupacaoDTO` | `200` | `400` |

### Fornecedores

| Verbo | Caminho | O que faz | Regra | Entrada | Saída | Sucesso | Erros |
|---|---|---|---|---|---|---|---|
| `POST` | `/fornecedores` | Cadastra um fornecedor | — | `FornecedorRequestDTO` | `FornecedorResponseDTO` | `201` | `400` |
| `GET` | `/fornecedores` | Lista os fornecedores | — | filtros `nome` (contém), `cnpj` (começa com) | página de `FornecedorResponseDTO` | `200` | `400` |
| `GET` | `/fornecedores/{id}` | Busca um fornecedor | — | — | `FornecedorResponseDTO` | `200` | `404` |
| `PUT` | `/fornecedores/{id}` | Altera um fornecedor | — | `FornecedorRequestDTO` | `FornecedorResponseDTO` | `200` | `400`, `404` |
| `DELETE` | `/fornecedores/{id}` | Exclui um fornecedor | R4 | — | — | `204` | `400`, `404` |

### Agendamentos

| Verbo | Caminho | O que faz | Regra | Entrada | Saída | Sucesso | Erros |
|---|---|---|---|---|---|---|---|
| `POST` | `/agendamentos` | Agenda uma descarga | R1, R2, R3 | `AgendamentoRequestDTO` | `AgendamentoResponseDTO` | `201` | `400`, `404` |
| `GET` | `/agendamentos` | Lista os agendamentos | — | filtros `docaId`, `fornecedorId`, `cargaRefrigerada`, `de`, `ate` | página de `AgendamentoResponseDTO` | `200` | `400` |
| `GET` | `/agendamentos/{id}` | Busca um agendamento | — | — | `AgendamentoResponseDTO` | `200` | `404` |
| `PUT` | `/agendamentos/{id}` | Remarca um agendamento | R1, R2, R3 | `AgendamentoRequestDTO` | `AgendamentoResponseDTO` | `200` | `400`, `404` |
| `DELETE` | `/agendamentos/{id}` | Cancela um agendamento | — | — | — | `204` | `404` |

### Observabilidade (carta C2)

| Verbo | Caminho | O que faz |
|---|---|---|
| `GET` | `/actuator/health` | `UP`/`DOWN` da API, com o detalhe do banco e do disco |
| `GET` | `/actuator/metrics` | Lista das métricas disponíveis |
| `GET` | `/actuator/prometheus` | Métricas em texto para o Prometheus, incluindo `agenda_operacoes_total` e `agenda_regras_violadas_total` |

## DTOs

| DTO | Direção | Campos | Validações |
|---|---|---|---|
| `DocaRequestDTO` | entrada | `identificacao`, `refrigerada` | os dois obrigatórios; `identificacao` com até 20 caracteres |
| `DocaResponseDTO` | saída | `id`, `identificacao`, `refrigerada` | — |
| `FornecedorRequestDTO` | entrada | `nome`, `cnpj` | os dois obrigatórios; `cnpj` com até 18 caracteres |
| `FornecedorResponseDTO` | saída | `id`, `nome`, `cnpj` | — |
| `AgendamentoRequestDTO` | entrada | `fornecedorId`, `docaId`, `inicio`, `fim`, `cargaRefrigerada` | todos obrigatórios |
| `AgendamentoResponseDTO` | saída | `id`, `fornecedor`, `doca`, `inicio`, `fim`, `cargaRefrigerada` | — |
| `OcupacaoDTO` | saída | `de`, `ate`, `horasDisponiveisPorDoca`, `taxaOcupacaoGeral`, `docas`, `alertas` | — |
| `OcupacaoDocaDTO` | saída | `docaId`, `identificacao`, `refrigerada`, `agendamentos`, `horasAgendadas`, `taxaOcupacao`, `horasDeCargaSecaEmDocaRefrigerada` | — |
| `ErroDTO` | saída | `status`, `erro`, `mensagem`, `caminho`, `timestamp`, `campos` | — |

As listagens devolvem uma página:

```json
{
  "content": [ ... ],
  "page": { "size": 10, "number": 0, "totalElements": 3, "totalPages": 1 }
}
```

Os exemplos de requisição e resposta de cada rota estão no [README](../README.md#6-rotas).

## Erros

Todo erro sai no mesmo formato. Este é o `404` de `GET /docas/999`:

```json
{
  "status": 404,
  "erro": "Not Found",
  "mensagem": "Doca não encontrada: 999",
  "caminho": "/docas/999",
  "timestamp": "2026-12-01T19:30:00",
  "campos": []
}
```

No `400` de validação, a lista `campos` traz um item por campo inválido: `{"campo": "identificacao", "mensagem": "Identificação é obrigatória"}`. Quando uma regra de negócio barra o pedido, o `400` vem com a regra na `mensagem` e `campos` vazio.

| Status | Quando acontece neste projeto |
|---|---|
| `400` | JSON ilegível ou campo inválido no DTO de entrada |
| `400` | Parâmetro em formato errado, parâmetro obrigatório ausente ou `sort` por campo inexistente |
| `400` | Regra R1, R2, R3 ou R4 violada, `fim` que não vem depois do `inicio`, ou período de ocupação inválido. A `mensagem` explica o motivo |
| `404` | Id de doca, fornecedor ou agendamento que não existe, ou rota inexistente |
| `405` | Verbo não suportado na rota |
| `500` | Falha inesperada. A mensagem é genérica; o detalhe fica no log `ERROR` com o mesmo `X-Request-Id` |
