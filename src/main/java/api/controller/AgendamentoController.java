// Define o pacote responsável pelas rotas HTTP da aplicação.
package api.controller;

// Importa a anotação que agrupa os endpoints no Swagger.
import io.swagger.v3.oas.annotations.tags.Tag;

// Importa as anotações de rota.
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Importa a camada que contém as regras dos agendamentos.
import api.service.AgendamentoService;

// CAMADA: Controller
// RESPONSABILIDADE: receber as requisições de /agendamentos. As regras R1, R2 e
//                   R3 não ficam aqui: ficam no AgendamentoService.
// ROTAS ATENDIDAS: as de /agendamentos em docs/contrato-da-api.md.
// SITUAÇÃO: montada. A lógica entra quando a rota for implementada.
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
}
