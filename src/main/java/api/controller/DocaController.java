// Define o pacote responsável pelas rotas HTTP da aplicação.
package api.controller;

// Importa o tipo de data usado no período da ocupação.
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

// Faz o Swagger mostrar page, size e sort como parâmetros soltos, e não como um objeto.
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

// Importa os DTOs de entrada, de saída e de erro -- e só eles: nenhum import de api.model.
import api.dto.DocaRequestDTO;
import api.dto.DocaResponseDTO;
import api.dto.ErroDTO;
import api.dto.OcupacaoDTO;
// Importa as camadas que contêm as operações da doca e o cálculo da ocupação.
import api.service.DocaService;
import api.service.OcupacaoService;

// CAMADA: Controller
// RESPONSABILIDADE: receber as requisições de /docas, validar o DTO de
//                   entrada e devolver o status certo. Não tem regra de negócio.
// ROTAS ATENDIDAS: POST /docas, GET /docas, GET /docas/{id}, PUT /docas/{id}, DELETE /docas/{id}
//                  e GET /docas/ocupacao (aplicação prática: Visualização de Dados).
// SITUAÇÃO: implementada.
// Agrupa todos os endpoints desta classe sob "Docas" no Swagger UI.
@Tag(name = "Docas", description = "Cadastro e consulta das docas do centro de distribuição")
// Informa ao Spring que a classe atende requisições REST.
@RestController
// Adiciona /docas antes de todas as rotas abaixo.
@RequestMapping("/docas")
public class DocaController {

    // Guarda a camada de serviço usada pelas rotas.
    private final DocaService service;

    // Calcula a ocupação das docas (Visualização de Dados).
    private final OcupacaoService ocupacaoService;

    // Recebe os services injetados pelo Spring.
    public DocaController(DocaService service, OcupacaoService ocupacaoService) {
        // Guarda os services para uso nos métodos HTTP.
        this.service = service;
        this.ocupacaoService = ocupacaoService;
    }

    // @Valid dispara a validação do DocaRequestDTO antes deste método rodar.
    @Operation(summary = "Cadastra uma doca", description = "O id é definido pela API, não pelo cliente.")
    @ApiResponse(responseCode = "201", description = "Doca cadastrada: o corpo já traz o id gerado")
    @ApiResponse(responseCode = "400", description = "Corpo inválido: a lista \"campos\" diz o que corrigir",
            content = @Content(schema = @Schema(implementation = ErroDTO.class)))
    @PostMapping
    public ResponseEntity<DocaResponseDTO> criar(@Valid @RequestBody DocaRequestDTO dto) {
        // Delega o cadastro para o service e devolve 201 Created com a doca no corpo.
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dto));
    }

    // Mapeia GET /docas?identificacao=&refrigerada=&page=&size=&sort=.
    @Operation(summary = "Lista as docas, paginado e com filtros opcionais",
            description = "Sem docas, a página vem vazia, não 404. Ordenação: sort=identificacao,asc")
    @ApiResponse(responseCode = "200", description = "Uma página de docas (pode vir vazia)")
    @ApiResponse(responseCode = "400", description = "Campo de ordenação inexistente",
            content = @Content(schema = @Schema(implementation = ErroDTO.class)))
    @GetMapping
    public Page<DocaResponseDTO> listar(
            @Parameter(description = "Parte da identificação, sem diferenciar maiúscula", example = "D-0")
            @RequestParam(required = false) String identificacao,
            @Parameter(description = "Só docas com (true) ou sem (false) câmara fria")
            @RequestParam(required = false) Boolean refrigerada,
            @ParameterObject @PageableDefault(size = 10, sort = "identificacao") Pageable pageable) {
        // Delega a listagem para o service.
        return service.listar(identificacao, refrigerada, pageable);
    }

    // Mapeia GET /docas/ocupacao?de=&ate=. O caminho fixo "ocupacao" tem prioridade
    // sobre /docas/{id}, então não há conflito entre as duas rotas.
    @Operation(summary = "Ocupação das docas num período (dados para dashboard)",
            description = "Para cada doca: agendamentos, horas reservadas, taxa de ocupação e horas de câmara "
                    + "fria gastas com carga seca. Traz também a taxa geral e alertas. Período de até 31 dias.")
    @ApiResponse(responseCode = "200", description = "Ocupação calculada")
    @ApiResponse(responseCode = "400", description = "Período inválido, maior que 31 dias ou parâmetro ausente",
            content = @Content(schema = @Schema(implementation = ErroDTO.class)))
    @GetMapping("/ocupacao")
    public OcupacaoDTO ocupacao(
            @Parameter(description = "Primeiro dia do período", example = "2026-12-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @Parameter(description = "Último dia do período (inclusive)", example = "2026-12-02")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        // Delega o cálculo para o service.
        return ocupacaoService.calcular(de, ate);
    }

    // Mapeia GET /docas/{id}.
    @Operation(summary = "Busca uma doca pelo id")
    @ApiResponse(responseCode = "200", description = "Doca encontrada")
    @ApiResponse(responseCode = "404", description = "Não existe doca com esse id",
            content = @Content(schema = @Schema(implementation = ErroDTO.class)))
    @GetMapping("/{id}")
    public DocaResponseDTO buscarPorId(@Parameter(description = "Id da doca", example = "1") @PathVariable Long id) {
        // Delega a busca para o service; se não existir, a exceção vira 404.
        return service.buscarPorId(id);
    }

    // Mapeia PUT /docas/{id}.
    @Operation(summary = "Altera uma doca")
    @ApiResponse(responseCode = "200", description = "Doca alterada")
    @ApiResponse(responseCode = "400", description = "Corpo inválido",
            content = @Content(schema = @Schema(implementation = ErroDTO.class)))
    @ApiResponse(responseCode = "404", description = "Não existe doca com esse id",
            content = @Content(schema = @Schema(implementation = ErroDTO.class)))
    @PutMapping("/{id}")
    public DocaResponseDTO atualizar(@Parameter(description = "Id da doca", example = "1") @PathVariable Long id,
                                     @Valid @RequestBody DocaRequestDTO dto) {
        // Delega a alteração para o service.
        return service.atualizar(id, dto);
    }

    // Mapeia DELETE /docas/{id}.
    @Operation(summary = "Exclui uma doca", description = "R4: doca com agendamentos não pode ser excluída.")
    @ApiResponse(responseCode = "204", description = "Doca excluída")
    @ApiResponse(responseCode = "400", description = "R4: a doca tem agendamentos",
            content = @Content(schema = @Schema(implementation = ErroDTO.class)))
    @ApiResponse(responseCode = "404", description = "Não existe doca com esse id",
            content = @Content(schema = @Schema(implementation = ErroDTO.class)))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@Parameter(description = "Id da doca", example = "1") @PathVariable Long id) {
        // Delega a exclusão para o service e devolve 204 No Content (sem corpo).
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
