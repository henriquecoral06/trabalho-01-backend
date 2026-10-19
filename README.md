# Agenda de Docas

> **Este é o ponto de partida do projeto do grupo.** Apague este bloco antes de entregar.
>
> **Objetivo do exercício:** planejar e organizar um backend antes de programar tudo. O grupo cria o problema e as regras de negócio, desenha o sistema, monta a estrutura de todas as entidades, faz a API funcionar com as regras implementadas e documenta tudo neste README. O enunciado completo está em `Aula 10-11/README.md`, no repositório da disciplina.
>
> **O que fazer, na ordem:**
>
> 1. Copie esta pasta inteira para fora do repositório da disciplina, com o nome do projeto, e publique no GitHub. Os comandos estão no enunciado (seção "Como começar pelo template").
> 2. Procure por `TROQUE` no projeto (`pom.xml`, `docker-compose.yml`, `OpenApiConfig.java`) e coloque o nome do projeto.
> 3. Preencha a seção [O problema](#o-problema) deste README: o problema que o grupo criou e as regras de negócio.
> 4. Preencha `docs/modelo-de-dominio.md` e `docs/contrato-da-api.md`.
> 5. Crie as tabelas e as classes de todas as entidades. O passo a passo está em [src/main/java/api/README.md](src/main/java/api/README.md).
> 6. Faça a API simples e implemente as regras de negócio.
> 7. Termine este README: troque tudo o que está entre `{{ }}` e apague o que está marcado com *(exemplo)*. Mantenha as seções e a ordem.
> 8. Abra o repositório no GitHub, leia o README como quem nunca viu o projeto e preencha `docs/autoavaliacao.md`.
>
> Na primeira vez que a API sobe, antes de existir tabela e rota, o Flyway avisa `No migrations found` e o Swagger UI mostra `No operations defined in spec!`. É o esperado.
>
> Os exemplos marcados com *(exemplo)* são de uma biblioteca. O projeto completo dela está em `Aula 10-11/exemplo-biblioteca`.

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
| {{Nome completo}} | [@{{usuario}}](https://github.com/{{usuario}}) |
| {{Nome completo}} | [@{{usuario}}](https://github.com/{{usuario}}) |
| {{Nome completo}} | [@{{usuario}}](https://github.com/{{usuario}}) |

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

**Mapa da estrutura.** Uma linha por entidade, todas as do modelo. Em cada célula, a classe e a situação: **pronta**, **implementada** (já tem lógica), **em parte** (só uma parte das rotas) ou **montada** (criada, ligada às outras, ainda sem lógica).

| Entidade | Migration | Model | Repository | DTOs | Mapper | Service | Controller |
|---|---|---|---|---|---|---|---|
| Livro *(exemplo)* | `V1__criar_livros.sql`, pronta | `Livro`, pronta | `LivroRepository`, pronto | `LivroRequestDTO`, `LivroResponseDTO`, prontos | `LivroMapper`, implementado | `LivroService`, implementado | `LivroController`, implementado |
| ... | ... | ... | ... | ... | ... | ... | ... |

## Entidades

| Entidade | O que representa | Como se relaciona |
|---|---|---|
| Livro *(exemplo)* | Um exemplar do acervo que pode ser emprestado | Aparece em vários empréstimos |
| ... | ... | ... |

Atributos, tipos e o diagrama das tabelas estão em [docs/modelo-de-dominio.md](docs/modelo-de-dominio.md).

## Rotas

### As que já funcionam

| Verbo | Caminho | O que faz | Regra | Sucesso | Erros |
|---|---|---|---|---|---|
| `POST` *(exemplo)* | `/livros` | Cadastra um livro | Nenhuma | `201` | `400` |
| `POST` *(exemplo)* | `/emprestimos` | Registra um empréstimo | R1 e R2 | `201` | `400`, `404` |
| ... | ... | ... | ... | ... | ... |

Exemplo de requisição *(exemplo: troque por uma rota do grupo)*:

```http
POST /livros
Content-Type: application/json

{
  "titulo": "Dom Casmurro",
  "isbn": "978-85-359-0277-5"
}
```

Resposta, `201 Created`:

```json
{
  "id": 1,
  "titulo": "Dom Casmurro",
  "isbn": "978-85-359-0277-5",
  "disponivel": true
}
```

### Erros

Todo erro sai no mesmo formato. Este é o `400` de uma regra de negócio violada *(exemplo)*:

```json
{
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "R2: o livro já está emprestado",
  "caminho": "/emprestimos",
  "timestamp": "2026-10-06T19:30:00",
  "campos": []
}
```

Um campo inválido também devolve `400`, com a lista `campos` dizendo qual campo corrigir. Um id que não existe devolve `404`.

### As planejadas

Estão no contrato, mas ainda não respondem.

| Recurso | O que vai fazer |
|---|---|
| ... | ... |

O contrato completo, com todas as rotas e os DTOs, está em [docs/contrato-da-api.md](docs/contrato-da-api.md).

## Como executar

Pré-requisitos: JDK 25, Docker Desktop aberto e Git.

```bash
git clone https://github.com/{{usuario}}/{{repositorio}}.git
cd {{repositorio}}

docker compose up -d          # sobe o PostgreSQL na porta 5437
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run
```

Com a API no ar:

| Endereço | O que é |
|---|---|
| <http://localhost:8080/swagger-ui.html> | Swagger UI: as rotas, com **Try it out** para enviar requisições |
| <http://localhost:8080/v3/api-docs> | O contrato OpenAPI em JSON |

Para testar as rotas, use o Swagger UI ou o **Postman**. No Postman, importe todas as rotas de uma vez: **Import** → **Link** → `http://localhost:8080/v3/api-docs`.

Para parar: `Ctrl+C` na API e `docker compose stop` no banco. Para apagar o banco e rodar as migrations do zero: `docker compose down -v` e suba de novo.

## Situação do projeto

| Parte | Situação |
|---|---|
| Desenho | {{ex.: completo: problema e regras neste README, modelo e contrato em docs/}} |
| Estrutura | {{ex.: as 3 entidades têm migration, entidade, Repository, DTOs, Mapper, Service e Controller}} |
| API e regras | {{ex.: as rotas de livros funcionam; R1, R2 e R3 estão implementadas}} |

**Ainda não funciona:** {{as rotas planejadas que ainda não respondem}}

## Documentação

| Documento | O que tem |
|---|---|
| [Modelo de domínio](docs/modelo-de-dominio.md) | Entidades, relacionamentos, diagrama das tabelas, migrations |
| [Contrato da API](docs/contrato-da-api.md) | Todas as rotas, os DTOs, exemplos de JSON e erros |
| [Autoavaliação](docs/autoavaliacao.md) | Os critérios da revisão do código, marcados pelo grupo |
