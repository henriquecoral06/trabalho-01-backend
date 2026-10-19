// Define o pacote da camada de regras de negócio.
package api.service;

// Importa o tipo de data e hora usado nas regras.
import java.time.LocalDateTime;

// Importa a anotação que registra a classe como serviço Spring.
import org.springframework.stereotype.Service;
// Importa a anotação que faz o método inteiro valer como uma operação só no banco.
import org.springframework.transaction.annotation.Transactional;

// Importa os DTOs de entrada e de saída do agendamento.
import api.dto.AgendamentoRequestDTO;
import api.dto.AgendamentoResponseDTO;
// Importa o mapper que monta a resposta.
import api.mapper.AgendamentoMapper;
// Importa as entidades envolvidas nas regras.
import api.model.Agendamento;
import api.model.Doca;
import api.model.Fornecedor;
// Importa os repositórios usados pelas regras.
import api.repository.AgendamentoRepository;
import api.repository.DocaRepository;
import api.repository.FornecedorRepository;

// CAMADA: Service
// RESPONSABILIDADE: regras de negócio dos agendamentos.
// REGRAS: R1 (a doca não pode ter dois agendamentos com horários sobrepostos),
//         R2 (carga refrigerada só em doca refrigerada) e
//         R3 (no mínimo 24 horas de antecedência).
// ROTAS ATENDIDAS: POST /agendamentos. GET /agendamentos e GET /agendamentos/{id}
//                  estão no contrato e entram na próxima etapa.
// SITUAÇÃO: implementada em parte. Só o POST, que é a rota das regras.
@Service
public class AgendamentoService {

    // R3: com quantas horas de antecedência, no mínimo, a descarga precisa ser agendada.
    private static final int ANTECEDENCIA_MINIMA_HORAS = 24;

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

    // POST /agendamentos. @Transactional: a consulta da R1 e a gravação acontecem
    // na mesma operação do banco.
    @Transactional
    public AgendamentoResponseDTO criar(AgendamentoRequestDTO dto) {
        // Entra um id, o Service busca a entidade: 404 se o fornecedor não existir.
        Fornecedor fornecedor = fornecedorRepository.findById(dto.fornecedorId())
                .orElseThrow(() -> new FornecedorNaoEncontradoException(dto.fornecedorId()));
        // O mesmo para a doca.
        Doca doca = docaRepository.findById(dto.docaId())
                .orElseThrow(() -> new DocaNaoEncontradaException(dto.docaId()));

        // Sem um horário válido, nenhuma regra faz sentido: o fim vem depois do início.
        if (!dto.fim().isAfter(dto.inicio())) {
            // 400: o pedido é válido no formato, mas não faz sentido para o sistema.
            throw new RegraDeNegocioException("O fim do agendamento precisa ser depois do início");
        }
        // R3: a descarga precisa ser agendada com no mínimo 24 horas de antecedência.
        if (dto.inicio().isBefore(LocalDateTime.now().plusHours(ANTECEDENCIA_MINIMA_HORAS))) {
            // A mensagem vai para o cliente, dentro do ErroDTO, com status 400.
            throw new RegraDeNegocioException("R3: o agendamento precisa ser feito com no mínimo "
                    + ANTECEDENCIA_MINIMA_HORAS + " horas de antecedência");
        }
        // R2: carga refrigerada só pode ir para doca com câmara fria.
        if (dto.cargaRefrigerada() && !doca.isRefrigerada()) {
            // Mesma coisa: 400 com a regra explicada.
            throw new RegraDeNegocioException("R2: a carga é refrigerada e a doca " + doca.getIdentificacao()
                    + " não é refrigerada");
        }
        // R1: a doca não pode ter outro agendamento que se sobreponha a este horário.
        repository.findFirstByDocaIdAndInicioBeforeAndFimAfter(doca.getId(), dto.fim(), dto.inicio())
                .ifPresent(existente -> {
                    // Mostra o horário que já ocupa a doca, para o fornecedor escolher outro.
                    throw new RegraDeNegocioException("R1: a doca " + doca.getId() + " já tem um agendamento entre "
                            + existente.getInicio() + " e " + existente.getFim());
                });

        // Passou pelas três regras: grava o agendamento novo; o banco gera o id.
        Agendamento salvo = repository.save(new Agendamento(fornecedor, doca, dto.inicio(), dto.fim(),
                dto.cargaRefrigerada()));
        // A entidade para aqui: para cima vai só o DTO.
        return mapper.toResponse(salvo);
    }
}
