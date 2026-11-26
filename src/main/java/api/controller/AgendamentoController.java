// Define o pacote responsável pelas rotas HTTP da aplicação.
package api.controller;

// Importa o tipo de data usado nos filtros de período.
import java.time.LocalDate;

// Importa as anotações de documentação do Swagger/OpenAPI.
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
// Importa a anotação que dispara a validação do DTO.
import jakarta.validation.Valid;

// Faz o Swagger mostrar page, size e sort como parâmetros soltos.
import org.springdoc.core.annotations.ParameterObject;
// Importa os tipos de paginação do Spring Data.
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
// Importa a anotação que lê datas no formato yyyy-MM-dd.
import org.springframework.format.annotation.DateTimeFormat;
// Importa o status HTTP usado na criação.
import org.springframework.http.HttpStatus;
// Importa o tipo usado para responder com status e corpo.
import org.springframework.http.ResponseEntity;
// Importa as anotações de rota, de parâmetro e de corpo.
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Importa os DTOs de entrada, de saída e de erro.
import api.dto.AgendamentoRequestDTO;
import api.dto.AgendamentoResponseDTO;
import api.dto.ErroDTO;
// Importa a camada que contém as regras dos agendamentos.
import api.service.AgendamentoService;

// CAMADA: Controller
// RESPONSABILIDADE: receber as requisições de /agendamentos. As regras R1, R2 e
//                   R3 não ficam aqui: ficam no AgendamentoService.
// ROTAS ATENDIDAS: POST, GET, GET /{id}, PUT /{id} e DELETE /{id} de /agendamentos.
// SITUAÇÃO: implementada.
// Agrupa os endpoints desta classe sob "Agendamentos" no Swagger UI.
@Tag(name = "Agendamentos", description = "Agendamento da descarga nas docas, com as regras R1, R2 e R3")
// Informa ao Spring que a classe atende requisições REST.
@RestController
// Adiciona /agendamentos antes de todas as rotas abaixo.
@RequestMapping("/agendamentos")
public class AgendamentoController {

    // Guarda a camada de serviço usada pelas rotas.
    private final AgendamentoService service;

    // Recebe o service injetado pelo Spring.
    public AgendamentoController(AgendamentoService service) {
        // Guarda o service para uso nos métodos HTTP.
        this.service = service;
    }

    // @Valid confere o formato; as regras de negócio quem confere é o Service.
    @Operation(summary = "Agenda a descarga de um fornecedor numa doca",
            description = "R1: a doca não pode ter outro agendamento no mesmo horário. "
                    + "R2: carga refrigerada só em doca refrigerada. "
                    + "R3: o início precisa estar pelo menos 24 horas à frente.")
    @ApiResponse(responseCode = "201", description = "Agendamento registrado")
    @ApiResponse(responseCode = "400", description = "Corpo inválido, ou regra R1, R2 ou R3 violada (a mensagem diz qual)",
            content = @Content(schema = @Schema(implementation = ErroDTO.class)))
    @ApiResponse(responseCode = "404", description = "O fornecedor ou a doca não existe",
            content = @Content(schema = @Schema(implementation = ErroDTO.class)))
    @PostMapping
    public ResponseEntity<AgendamentoResponseDTO> criar(@Valid @RequestBody AgendamentoRequestDTO dto) {
        // Delega para o service, que aplica as regras, e devolve 201 Created.
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dto));
    }

    // Mapeia GET /agendamentos com os filtros opcionais.
    @Operation(summary = "Lista os agendamentos, paginado e com filtros opcionais",
            description = "de/ate filtram pelo dia do início (ate entra inteiro). Ordenação padrão: inicio,asc")
    @ApiResponse(responseCode = "200", description = "Uma página de agendamentos (pode vir vazia)")
    @ApiResponse(responseCode = "400", description = "Parâmetro em formato errado ou campo de ordenação inexistente",
            content = @Content(schema = @Schema(implementation = ErroDTO.class)))
    @GetMapping
    public Page<AgendamentoResponseDTO> listar(
            @Parameter(description = "Só os agendamentos desta doca", example = "1")
            @RequestParam(required = false) Long docaId,
            @Parameter(description = "Só os agendamentos deste fornecedor", example = "1")
            @RequestParam(required = false) Long fornecedorId,
            @Parameter(description = "Só cargas refrigeradas (true) ou secas (false)")
            @RequestParam(required = false) Boolean cargaRefrigerada,
            @Parameter(description = "Começam a partir deste dia", example = "2026-12-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @Parameter(description = "Começam até este dia (inclusive)", example = "2026-12-05")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @ParameterObject @PageableDefault(size = 10, sort = "inicio") Pageable pageable) {
        // Delega a listagem para o service.
        return service.listar(docaId, fornecedorId, cargaRefrigerada, de, ate, pageable);
    }

    // Mapeia GET /agendamentos/{id}.
    @Operation(summary = "Busca um agendamento pelo id")
    @ApiResponse(responseCode = "200", description = "Agendamento encontrado")
    @ApiResponse(responseCode = "404", description = "Não existe agendamento com esse id",
            content = @Content(schema = @Schema(implementation = ErroDTO.class)))
    @GetMapping("/{id}")
    public AgendamentoResponseDTO buscarPorId(
            @Parameter(description = "Id do agendamento", example = "1") @PathVariable Long id) {
        // Delega a busca para o service; se não existir, a exceção vira 404.
        return service.buscarPorId(id);
    }

    // Mapeia PUT /agendamentos/{id}: remarcação.
    @Operation(summary = "Remarca um agendamento", description = "As regras R1, R2 e R3 valem de novo.")
    @ApiResponse(responseCode = "200", description = "Agendamento remarcado")
    @ApiResponse(responseCode = "400", description = "Corpo inválido, ou regra R1, R2 ou R3 violada",
            content = @Content(schema = @Schema(implementation = ErroDTO.class)))
    @ApiResponse(responseCode = "404", description = "O agendamento, o fornecedor ou a doca não existe",
            content = @Content(schema = @Schema(implementation = ErroDTO.class)))
    @PutMapping("/{id}")
    public AgendamentoResponseDTO atualizar(
            @Parameter(description = "Id do agendamento", example = "1") @PathVariable Long id,
            @Valid @RequestBody AgendamentoRequestDTO dto) {
        // Delega a remarcação para o service.
        return service.atualizar(id, dto);
    }

    // Mapeia DELETE /agendamentos/{id}: cancelamento.
    @Operation(summary = "Cancela um agendamento", description = "O horário volta a ficar livre na doca.")
    @ApiResponse(responseCode = "204", description = "Agendamento cancelado")
    @ApiResponse(responseCode = "404", description = "Não existe agendamento com esse id",
            content = @Content(schema = @Schema(implementation = ErroDTO.class)))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @Parameter(description = "Id do agendamento", example = "1") @PathVariable Long id) {
        // Delega o cancelamento para o service e devolve 204 No Content.
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
