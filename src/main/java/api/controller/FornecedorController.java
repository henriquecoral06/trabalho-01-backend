// Define o pacote responsável pelas rotas HTTP da aplicação.
package api.controller;

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
import api.dto.ErroDTO;
import api.dto.FornecedorRequestDTO;
import api.dto.FornecedorResponseDTO;
// Importa a camada que contém as operações do fornecedor.
import api.service.FornecedorService;

// CAMADA: Controller
// RESPONSABILIDADE: receber as requisições de /fornecedores. Não tem regra de negócio.
// ROTAS ATENDIDAS: POST, GET, GET /{id}, PUT /{id} e DELETE /{id} de /fornecedores.
// SITUAÇÃO: implementada.
// Agrupa os endpoints desta classe sob "Fornecedores" no Swagger UI.
@Tag(name = "Fornecedores", description = "Cadastro e consulta de fornecedores")
// Informa ao Spring que a classe atende requisições REST.
@RestController
// Adiciona /fornecedores antes de todas as rotas abaixo.
@RequestMapping("/fornecedores")
public class FornecedorController {

    // Guarda a camada de serviço usada pelas rotas.
    private final FornecedorService service;

    // Recebe o service injetado pelo Spring.
    public FornecedorController(FornecedorService service) {
        // Guarda o service para uso nos métodos HTTP.
        this.service = service;
    }

    // @Valid dispara a validação do FornecedorRequestDTO antes deste método rodar.
    @Operation(summary = "Cadastra um fornecedor")
    @ApiResponse(responseCode = "201", description = "Fornecedor cadastrado: o corpo já traz o id gerado")
    @ApiResponse(responseCode = "400", description = "Corpo inválido: a lista \"campos\" diz o que corrigir",
            content = @Content(schema = @Schema(implementation = ErroDTO.class)))
    @PostMapping
    public ResponseEntity<FornecedorResponseDTO> criar(@Valid @RequestBody FornecedorRequestDTO dto) {
        // Delega o cadastro para o service e devolve 201 Created.
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dto));
    }

    // Mapeia GET /fornecedores?nome=&cnpj=&page=&size=&sort=.
    @Operation(summary = "Lista os fornecedores, paginado e com filtros opcionais",
            description = "Ordenação: sort=nome,asc")
    @ApiResponse(responseCode = "200", description = "Uma página de fornecedores (pode vir vazia)")
    @ApiResponse(responseCode = "400", description = "Campo de ordenação inexistente",
            content = @Content(schema = @Schema(implementation = ErroDTO.class)))
    @GetMapping
    public Page<FornecedorResponseDTO> listar(
            @Parameter(description = "Parte do nome, sem diferenciar maiúscula", example = "serra")
            @RequestParam(required = false) String nome,
            @Parameter(description = "Começo do CNPJ", example = "12.345")
            @RequestParam(required = false) String cnpj,
            @ParameterObject @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        // Delega a listagem para o service.
        return service.listar(nome, cnpj, pageable);
    }

    // Mapeia GET /fornecedores/{id}.
    @Operation(summary = "Busca um fornecedor pelo id")
    @ApiResponse(responseCode = "200", description = "Fornecedor encontrado")
    @ApiResponse(responseCode = "404", description = "Não existe fornecedor com esse id",
            content = @Content(schema = @Schema(implementation = ErroDTO.class)))
    @GetMapping("/{id}")
    public FornecedorResponseDTO buscarPorId(
            @Parameter(description = "Id do fornecedor", example = "1") @PathVariable Long id) {
        // Delega a busca para o service; se não existir, a exceção vira 404.
        return service.buscarPorId(id);
    }

    // Mapeia PUT /fornecedores/{id}.
    @Operation(summary = "Altera um fornecedor")
    @ApiResponse(responseCode = "200", description = "Fornecedor alterado")
    @ApiResponse(responseCode = "400", description = "Corpo inválido",
            content = @Content(schema = @Schema(implementation = ErroDTO.class)))
    @ApiResponse(responseCode = "404", description = "Não existe fornecedor com esse id",
            content = @Content(schema = @Schema(implementation = ErroDTO.class)))
    @PutMapping("/{id}")
    public FornecedorResponseDTO atualizar(
            @Parameter(description = "Id do fornecedor", example = "1") @PathVariable Long id,
            @Valid @RequestBody FornecedorRequestDTO dto) {
        // Delega a alteração para o service.
        return service.atualizar(id, dto);
    }

    // Mapeia DELETE /fornecedores/{id}.
    @Operation(summary = "Exclui um fornecedor",
            description = "R4: fornecedor com agendamentos não pode ser excluído.")
    @ApiResponse(responseCode = "204", description = "Fornecedor excluído")
    @ApiResponse(responseCode = "400", description = "R4: o fornecedor tem agendamentos",
            content = @Content(schema = @Schema(implementation = ErroDTO.class)))
    @ApiResponse(responseCode = "404", description = "Não existe fornecedor com esse id",
            content = @Content(schema = @Schema(implementation = ErroDTO.class)))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @Parameter(description = "Id do fornecedor", example = "1") @PathVariable Long id) {
        // Delega a exclusão para o service e devolve 204 No Content.
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
