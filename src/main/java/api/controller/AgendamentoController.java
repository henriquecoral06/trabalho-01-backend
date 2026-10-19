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
import api.dto.AgendamentoRequestDTO;
import api.dto.AgendamentoResponseDTO;
import api.dto.ErroDTO;
// Importa a camada que contém as regras dos agendamentos.
import api.service.AgendamentoService;

// CAMADA: Controller
// RESPONSABILIDADE: receber as requisições de /agendamentos. As regras R1, R2 e
//                   R3 não ficam aqui: ficam no AgendamentoService.
// ROTAS ATENDIDAS: POST /agendamentos. GET /agendamentos e GET /agendamentos/{id}
//                  estão no contrato e entram na próxima etapa.
// SITUAÇÃO: implementada em parte.
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
}
