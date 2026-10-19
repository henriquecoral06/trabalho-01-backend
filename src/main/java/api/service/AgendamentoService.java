// Define o pacote da camada de regras de negócio.
package api.service;

// Importa a anotação que registra a classe como serviço Spring.
import org.springframework.stereotype.Service;

// Importa o mapper que monta a resposta.
import api.mapper.AgendamentoMapper;
// Importa os repositórios usados pelas regras.
import api.repository.AgendamentoRepository;
import api.repository.DocaRepository;
import api.repository.FornecedorRepository;

// CAMADA: Service
// RESPONSABILIDADE: regras de negócio dos agendamentos.
// REGRAS: R1 (a doca não pode ter dois agendamentos com horários sobrepostos),
//         R2 (carga refrigerada só em doca refrigerada) e
//         R3 (no mínimo 24 horas de antecedência).
// ROTAS ATENDIDAS: as de /agendamentos em docs/contrato-da-api.md.
// SITUAÇÃO: montada. A lógica entra quando a rota for implementada.
@Service
public class AgendamentoService {

    // Acesso à tabela agendamentos.
    private final AgendamentoRepository repository;

    // Usado para buscar o fornecedor pelo id que chegou no DTO.
    private final FornecedorRepository fornecedorRepository;

    // Usado para buscar a doca pelo id que chegou no DTO.
    private final DocaRepository docaRepository;

    // Monta a resposta, com fornecedor e doca completos.
    private final AgendamentoMapper mapper;

    // Recebe as dependências que o Spring injeta automaticamente.
    public AgendamentoService(AgendamentoRepository repository, FornecedorRepository fornecedorRepository,
                              DocaRepository docaRepository, AgendamentoMapper mapper) {
        // Guarda as dependências recebidas nos atributos da classe.
        this.repository = repository;
        this.fornecedorRepository = fornecedorRepository;
        this.docaRepository = docaRepository;
        this.mapper = mapper;
    }
}
