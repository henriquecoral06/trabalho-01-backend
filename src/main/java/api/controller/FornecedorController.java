// Define o pacote responsável pelas rotas HTTP da aplicação.
package api.controller;

// Importa as anotações de documentação do Swagger/OpenAPI.
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
// Importa a anotação que dispara a validação do DTO.
import jakarta.validation.Valid;

// Importa o status HTTP usado na criação.
import org.springframework.http.HttpStatus;
// Importa o tipo usado para responder com status e corpo.
import org.springframework.http.ResponseEntity;
// Importa as anotações de rota e de corpo.
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Importa os DTOs de entrada, de saída e de erro.
import api.dto.ErroDTO;
import api.dto.FornecedorRequestDTO;
import api.dto.FornecedorResponseDTO;
// Importa a camada que contém as operações do fornecedor.
import api.service.FornecedorService;

// CAMADA: Controller
// RESPONSABILIDADE: receber as requisições de /fornecedores.
// ROTAS ATENDIDAS: POST /fornecedores. GET /fornecedores e GET /fornecedores/{id}
//                  estão no contrato e entram na próxima etapa.
// SITUAÇÃO: implementada em parte.
// Agrupa os endpoints desta classe sob "Fornecedores" no Swagger UI.
@Tag(name = "Fornecedores", description = "Cadastro de fornecedores")
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
}
