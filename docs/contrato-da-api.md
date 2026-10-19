# Contrato da API

## Convenções

| Item | Valor |
|---|---|
| Endereço base | `http://localhost:8080` |
| Formato | JSON, na requisição e na resposta |
| Data e hora | `yyyy-MM-ddTHH:mm:ss`, por exemplo `2026-10-27T08:00:00` |
| Erros | Sempre no formato `ErroDTO` (ver [Erros](#erros)) |
| Documentação viva | `http://localhost:8080/swagger-ui.html` |

## Rotas

Coluna **Funciona**: `sim` para as rotas que já respondem nesta etapa: as três da API simples (docas) e as que as regras de negócio usam.

### Docas

| Verbo | Caminho | O que faz | Entrada | Saída | Sucesso | Erros | Funciona |
|---|---|---|---|---|---|---|---|
| `POST` | `/docas` | Cadastra uma doca | `DocaRequestDTO` | `DocaResponseDTO` | `201` | `400` | sim |
| `GET` | `/docas` | Lista as docas | Nenhuma | lista de `DocaResponseDTO` | `200` | Nenhum | sim |
| `GET` | `/docas/{id}` | Busca uma doca | Nenhuma | `DocaResponseDTO` | `200` | `404` | sim |

### Fornecedores

| Verbo | Caminho | O que faz | Entrada | Saída | Sucesso | Erros | Funciona |
|---|---|---|---|---|---|---|---|
| `POST` | `/fornecedores` | Cadastra um fornecedor. Sem fornecedor não há agendamento | `FornecedorRequestDTO` | `FornecedorResponseDTO` | `201` | `400` | sim |
| `GET` | `/fornecedores` | Lista os fornecedores | Nenhuma | lista de `FornecedorResponseDTO` | `200` | Nenhum | não |
| `GET` | `/fornecedores/{id}` | Busca um fornecedor | Nenhuma | `FornecedorResponseDTO` | `200` | `404` | não |

### Agendamentos

O `POST` nasce das regras de negócio do README e já funciona.

| Verbo | Caminho | O que faz | Regra | Entrada | Saída | Sucesso | Erros | Funciona |
|---|---|---|---|---|---|---|---|---|
| `POST` | `/agendamentos` | Agenda a descarga de um fornecedor numa doca | R1, R2 e R3 | `AgendamentoRequestDTO` | `AgendamentoResponseDTO` | `201` | `400`, `404` | sim |
| `GET` | `/agendamentos` | Lista os agendamentos | Nenhuma | Nenhuma | lista de `AgendamentoResponseDTO` | `200` | Nenhum | não |
| `GET` | `/agendamentos/{id}` | Busca um agendamento | Nenhuma | Nenhuma | `AgendamentoResponseDTO` | `200` | `404` | não |

## DTOs

| DTO | Direção | Campos | Validações |
|---|---|---|---|
| `DocaRequestDTO` | entrada | `identificacao`, `refrigerada` | os dois obrigatórios; `identificacao` com até 20 caracteres |
| `DocaResponseDTO` | saída | `id`, `identificacao`, `refrigerada` | Nenhuma |
| `FornecedorRequestDTO` | entrada | `nome`, `cnpj` | os dois obrigatórios; `cnpj` com até 18 caracteres |
| `FornecedorResponseDTO` | saída | `id`, `nome`, `cnpj` | Nenhuma |
| `AgendamentoRequestDTO` | entrada | `fornecedorId`, `docaId`, `inicio`, `fim`, `cargaRefrigerada` | todos obrigatórios |
| `AgendamentoResponseDTO` | saída | `id`, `fornecedor`, `doca`, `inicio`, `fim`, `cargaRefrigerada` | Nenhuma |

## Exemplos

### `POST /docas` (funciona)

Requisição:

```json
{
  "identificacao": "D-01",
  "refrigerada": true
}
```

Resposta, `201 Created`:

```json
{
  "id": 1,
  "identificacao": "D-01",
  "refrigerada": true
}
```

### `POST /fornecedores` (funciona)

Requisição:

```json
{
  "nome": "Laticínios Serra Azul",
  "cnpj": "12.345.678/0001-90"
}
```

Resposta, `201 Created`:

```json
{
  "id": 1,
  "nome": "Laticínios Serra Azul",
  "cnpj": "12.345.678/0001-90"
}
```

### `POST /agendamentos` (funciona)

Entram só os ids; fornecedor e doca voltam completos.

Requisição:

```json
{
  "fornecedorId": 1,
  "docaId": 1,
  "inicio": "2026-10-27T08:00:00",
  "fim": "2026-10-27T09:00:00",
  "cargaRefrigerada": true
}
```

Resposta, `201 Created`:

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

## Erros

Todo erro sai no mesmo formato. Este é o `404` de `GET /docas/999`:

```json
{
  "status": 404,
  "erro": "Not Found",
  "mensagem": "Doca não encontrada: 999",
  "caminho": "/docas/999",
  "timestamp": "2026-10-20T19:30:00",
  "campos": []
}
```

No `400` de validação, a lista `campos` traz um item por campo inválido: `{"campo": "identificacao", "mensagem": "Identificação é obrigatória"}`.

Quando uma regra de negócio barra o pedido, o `400` vem com a regra na `mensagem` e `campos` vazio. Este é o de `POST /agendamentos` numa doca que já está ocupada no horário:

```json
{
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "R1: a doca 1 já tem um agendamento entre 2026-10-27T08:00 e 2026-10-27T09:00",
  "caminho": "/agendamentos",
  "timestamp": "2026-10-20T19:30:00",
  "campos": []
}
```

| Status | Quando acontece neste projeto |
|---|---|
| `400` | JSON ilegível ou campo inválido no DTO de entrada |
| `400` | Regra R1, R2 ou R3 violada, ou `fim` que não vem depois do `inicio`. A `mensagem` explica o motivo |
| `404` | Id de doca ou de fornecedor que não existe |
