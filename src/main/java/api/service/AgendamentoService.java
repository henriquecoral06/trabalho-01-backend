// Define o pacote da camada de regras de negócio.
package api.service;

// Importa os tipos de data usados nas regras e nos filtros.
import java.time.LocalDate;
import java.time.LocalDateTime;
// Importa o Optional devolvido pela consulta da R1.
import java.util.Optional;

// Importa a API de log (SLF4J): carta C2.
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// Importa os tipos de paginação do Spring Data.
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
// Importa o tipo que junta os filtros opcionais num WHERE só.
import org.springframework.data.jpa.domain.Specification;
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
// Importa os filtros opcionais da listagem.
import api.repository.Filtros;
// Importa o registro das operações e das regras violadas (métricas).
import api.observability.Operacoes;

// CAMADA: Service
// RESPONSABILIDADE: CRUD dos agendamentos e as regras de negócio deles.
// REGRAS: R1 (a doca não pode ter dois agendamentos com horários sobrepostos),
//         R2 (carga refrigerada só em doca refrigerada) e
//         R3 (no mínimo 24 horas de antecedência). Valem no POST e no PUT.
// ROTAS ATENDIDAS: POST, GET (paginado, com filtros), GET /{id}, PUT /{id} e DELETE /{id} de /agendamentos.
// LOGS (carta C2): INFO ao criar, remarcar e cancelar; WARN, com o código da
//                  regra, cada vez que R1, R2 ou R3 recusa um pedido.
// SITUAÇÃO: implementada.
@Service
public class AgendamentoService {

    // Logger desta classe.
    private static final Logger log = LoggerFactory.getLogger(AgendamentoService.class);

    // R3: com quantas horas de antecedência, no mínimo, a descarga precisa ser agendada.
    public static final int ANTECEDENCIA_MINIMA_HORAS = 24;

    // Acesso à tabela agendamentos.
    private final AgendamentoRepository repository;

    // Usado para buscar o fornecedor pelo id que chegou no DTO.
    private final FornecedorRepository fornecedorRepository;

    // Usado para buscar a doca pelo id que chegou no DTO.
    private final DocaRepository docaRepository;

    // Monta a resposta, com fornecedor e doca completos.
    private final AgendamentoMapper mapper;

    // Conta as operações e as regras violadas nas métricas.
    private final Operacoes operacoes;

    // Recebe as dependências que o Spring injeta automaticamente.
    public AgendamentoService(AgendamentoRepository repository, FornecedorRepository fornecedorRepository,
                              DocaRepository docaRepository, AgendamentoMapper mapper, Operacoes operacoes) {
        // Guarda as dependências recebidas nos atributos da classe.
        this.repository = repository;
        this.fornecedorRepository = fornecedorRepository;
        this.docaRepository = docaRepository;
        this.mapper = mapper;
        this.operacoes = operacoes;
    }

    // POST /agendamentos. @Transactional: a consulta da R1 e a gravação acontecem
    // na mesma operação do banco.
    @Transactional
    public AgendamentoResponseDTO criar(AgendamentoRequestDTO dto) {
        // Entra um id, o Service busca a entidade: 404 se não existir.
        Fornecedor fornecedor = buscarFornecedor(dto.fornecedorId());
        Doca doca = buscarDoca(dto.docaId());
        // Confere R1, R2 e R3. null: não há agendamento a ignorar na R1.
        validarRegras(dto, doca, null);
        // Passou pelas três regras: grava o agendamento novo; o banco gera o id.
        Agendamento salvo = repository.save(new Agendamento(fornecedor, doca, dto.inicio(), dto.fim(),
                dto.cargaRefrigerada()));
        registrar("criar", "criado", salvo);
        // A entidade para aqui: para cima vai só o DTO.
        return mapper.toResponse(salvo);
    }

    // GET /agendamentos: uma página de agendamentos, com os filtros opcionais.
    // de/ate são dias: "ate" entra inteiro (até 23:59:59 daquele dia).
    public Page<AgendamentoResponseDTO> listar(Long docaId, Long fornecedorId, Boolean cargaRefrigerada,
                                               LocalDate de, LocalDate ate, Pageable pageable) {
        // Junta os filtros: os que vieram null são ignorados.
        Specification<Agendamento> filtro = Specification.allOf(
                Filtros.agendamentoDaDoca(docaId),
                Filtros.agendamentoDoFornecedor(fornecedorId),
                Filtros.agendamentoCargaRefrigerada(cargaRefrigerada),
                Filtros.agendamentoComecaAPartirDe(de == null ? null : de.atStartOfDay()),
                Filtros.agendamentoComecaAntesDe(ate == null ? null : ate.plusDays(1).atStartOfDay()));
        // Page.map converte cada item e mantém os dados da paginação.
        return repository.findAll(filtro, pageable).map(mapper::toResponse);
    }

    // GET /agendamentos/{id}: devolve um agendamento ou lança a exceção que vira 404.
    public AgendamentoResponseDTO buscarPorId(Long id) {
        // Converte o agendamento encontrado.
        return mapper.toResponse(buscarEntidade(id));
    }

    // PUT /agendamentos/{id}: remarca o agendamento. As regras valem de novo,
    // porque o horário, a doca ou a carga podem ter mudado.
    @Transactional
    public AgendamentoResponseDTO atualizar(Long id, AgendamentoRequestDTO dto) {
        // 404 se o agendamento, o fornecedor ou a doca não existir.
        Agendamento agendamento = buscarEntidade(id);
        Fornecedor fornecedor = buscarFornecedor(dto.fornecedorId());
        Doca doca = buscarDoca(dto.docaId());
        // Confere R1, R2 e R3; na R1, o próprio agendamento não conta como conflito.
        validarRegras(dto, doca, id);
        // Copia os dados novos para a entidade e grava.
        agendamento.atualizar(fornecedor, doca, dto.inicio(), dto.fim(), dto.cargaRefrigerada());
        Agendamento salvo = repository.save(agendamento);
        registrar("atualizar", "remarcado", salvo);
        return mapper.toResponse(salvo);
    }

    // DELETE /agendamentos/{id}: cancela o agendamento e libera a doca.
    @Transactional
    public void excluir(Long id) {
        // 404 se o agendamento não existir.
        Agendamento agendamento = buscarEntidade(id);
        // Apaga: o horário volta a ficar livre para outro fornecedor.
        repository.delete(agendamento);
        registrar("excluir", "cancelado", agendamento);
    }

    // As regras do agendamento, num lugar só: POST e PUT usam as mesmas.
    // idIgnorado: no PUT, o id do próprio agendamento; no POST, null.
    private void validarRegras(AgendamentoRequestDTO dto, Doca doca, Long idIgnorado) {
        // Sem um horário válido, nenhuma regra faz sentido: o fim vem depois do início.
        if (!dto.fim().isAfter(dto.inicio())) {
            // 400: o pedido é válido no formato, mas não faz sentido para o sistema.
            throw new RegraDeNegocioException(operacoes.regraViolada("HORARIO", "agendamento",
                    "O fim do agendamento precisa ser depois do início"));
        }
        // R3: a descarga precisa ser agendada com no mínimo 24 horas de antecedência.
        if (dto.inicio().isBefore(LocalDateTime.now().plusHours(ANTECEDENCIA_MINIMA_HORAS))) {
            // A mensagem vai para o cliente, dentro do ErroDTO, com status 400.
            throw new RegraDeNegocioException(operacoes.regraViolada("R3", "agendamento",
                    "R3: o agendamento precisa ser feito com no mínimo " + ANTECEDENCIA_MINIMA_HORAS
                            + " horas de antecedência"));
        }
        // R2: carga refrigerada só pode ir para doca com câmara fria.
        if (dto.cargaRefrigerada() && !doca.isRefrigerada()) {
            // Mesma coisa: 400 com a regra explicada.
            throw new RegraDeNegocioException(operacoes.regraViolada("R2", "agendamento",
                    "R2: a carga é refrigerada e a doca " + doca.getIdentificacao() + " não é refrigerada"));
        }
        // R1: a doca não pode ter outro agendamento que se sobreponha a este horário.
        Optional<Agendamento> conflito = idIgnorado == null
                ? repository.findFirstByDocaIdAndInicioBeforeAndFimAfter(doca.getId(), dto.fim(), dto.inicio())
                : repository.findFirstByDocaIdAndIdNotAndInicioBeforeAndFimAfter(doca.getId(), idIgnorado,
                        dto.fim(), dto.inicio());
        // Mostra o horário que já ocupa a doca, para o fornecedor escolher outro.
        conflito.ifPresent(existente -> {
            throw new RegraDeNegocioException(operacoes.regraViolada("R1", "agendamento",
                    "R1: a doca " + doca.getIdentificacao() + " já tem um agendamento entre "
                            + existente.getInicio() + " e " + existente.getFim()));
        });
    }

    // Log INFO e métrica de uma operação que deu certo: criar, atualizar ou excluir.
    private void registrar(String operacao, String verbo, Agendamento a) {
        log.atInfo()
                .addKeyValue("operacao", operacao)
                .addKeyValue("agendamentoId", a.getId())
                .addKeyValue("docaId", a.getDoca().getId())
                .addKeyValue("fornecedorId", a.getFornecedor().getId())
                .log("Agendamento {} {}: fornecedor {}, doca {}, de {} a {} (carga refrigerada: {})",
                        a.getId(), verbo, a.getFornecedor().getNome(), a.getDoca().getIdentificacao(),
                        a.getInicio(), a.getFim(), a.isCargaRefrigerada());
        operacoes.contar("agendamento", operacao);
    }

    // Busca o agendamento ou lança a exceção que vira 404.
    private Agendamento buscarEntidade(Long id) {
        return repository.findById(id).orElseThrow(() -> new AgendamentoNaoEncontradoException(id));
    }

    // Busca o fornecedor ou lança a exceção que vira 404.
    private Fornecedor buscarFornecedor(Long id) {
        return fornecedorRepository.findById(id).orElseThrow(() -> new FornecedorNaoEncontradoException(id));
    }

    // Busca a doca ou lança a exceção que vira 404.
    private Doca buscarDoca(Long id) {
        return docaRepository.findById(id).orElseThrow(() -> new DocaNaoEncontradaException(id));
    }
}
