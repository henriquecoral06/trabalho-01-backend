// Define o pacote da camada de regras de negócio.
package api.service;

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

// Importa os DTOs de entrada e de saída da doca.
import api.dto.DocaRequestDTO;
import api.dto.DocaResponseDTO;
// Importa o mapper que traduz DTO <-> entidade.
import api.mapper.DocaMapper;
// Importa a entidade persistida.
import api.model.Doca;
// Importa os repositórios usados pelo service.
import api.repository.AgendamentoRepository;
import api.repository.DocaRepository;
// Importa os filtros opcionais da listagem.
import api.repository.Filtros;
// Importa o registro das operações e das regras violadas (métricas).
import api.observability.Operacoes;

// CAMADA: Service
// RESPONSABILIDADE: CRUD das docas. Recebe DTO, usa o mapper e devolve DTO:
//                   a entidade Doca não sobe para o Controller.
// REGRAS: R4 (doca com agendamentos não pode ser excluída). As regras do
//         agendamento (R1, R2 e R3) moram no AgendamentoService.
// ROTAS ATENDIDAS: POST, GET (paginado, com filtros), GET /{id}, PUT /{id} e DELETE /{id} de /docas.
// LOGS (carta C2): INFO ao criar, alterar e excluir; WARN quando a R4 barra a exclusão.
// SITUAÇÃO: implementada.
@Service
public class DocaService {

    // Logger desta classe: o nome "api.service.DocaService" aparece em cada linha.
    private static final Logger log = LoggerFactory.getLogger(DocaService.class);

    // Acesso à tabela docas.
    private final DocaRepository repository;

    // Usado pela R4, para saber se a doca tem agendamentos.
    private final AgendamentoRepository agendamentoRepository;

    // Converte DTO <-> entidade.
    private final DocaMapper mapper;

    // Conta as operações e as regras violadas nas métricas.
    private final Operacoes operacoes;

    // Recebe as dependências que o Spring injeta automaticamente.
    public DocaService(DocaRepository repository, AgendamentoRepository agendamentoRepository, DocaMapper mapper,
                       Operacoes operacoes) {
        // Guarda as dependências recebidas nos atributos da classe.
        this.repository = repository;
        this.agendamentoRepository = agendamentoRepository;
        this.mapper = mapper;
        this.operacoes = operacoes;
    }

    // POST /docas: monta a entidade, grava e devolve a doca com o id gerado.
    public DocaResponseDTO criar(DocaRequestDTO dto) {
        // O banco gera o id no INSERT e devolve a entidade preenchida.
        Doca salva = repository.save(mapper.toEntity(dto));
        // Log estruturado: a mensagem para gente ler, os campos para o log JSON.
        log.atInfo().addKeyValue("operacao", "criar").addKeyValue("docaId", salva.getId())
                .log("Doca {} criada: {} (refrigerada: {})", salva.getId(), salva.getIdentificacao(),
                        salva.isRefrigerada());
        operacoes.contar("doca", "criar");
        // A entidade para aqui: para cima vai só o DTO.
        return mapper.toResponse(salva);
    }

    // GET /docas: uma página de docas, com os filtros opcionais. Sem docas, a página vem vazia.
    public Page<DocaResponseDTO> listar(String identificacao, Boolean refrigerada, Pageable pageable) {
        // Junta os filtros: os que vieram null são ignorados.
        Specification<Doca> filtro = Specification.allOf(
                Filtros.docaIdentificacaoContem(identificacao),
                Filtros.docaRefrigerada(refrigerada));
        // Page.map converte cada item e mantém os dados da paginação.
        return repository.findAll(filtro, pageable).map(mapper::toResponse);
    }

    // GET /docas/{id}: devolve uma doca ou lança a exceção que vira 404.
    public DocaResponseDTO buscarPorId(Long id) {
        // Converte a doca encontrada.
        return mapper.toResponse(buscarEntidade(id));
    }

    // PUT /docas/{id}: troca os dados da doca.
    @Transactional
    public DocaResponseDTO atualizar(Long id, DocaRequestDTO dto) {
        // 404 se a doca não existir.
        Doca doca = buscarEntidade(id);
        // Guarda como era, para o log mostrar o antes e o depois.
        String antes = doca.getIdentificacao() + " (refrigerada: " + doca.isRefrigerada() + ")";
        // Copia os dados novos para a entidade.
        mapper.updateEntity(doca, dto);
        Doca salva = repository.save(doca);
        log.atInfo().addKeyValue("operacao", "atualizar").addKeyValue("docaId", id)
                .log("Doca {} alterada: {} -> {} (refrigerada: {})", id, antes, salva.getIdentificacao(),
                        salva.isRefrigerada());
        operacoes.contar("doca", "atualizar");
        // Devolve a doca atualizada.
        return mapper.toResponse(salva);
    }

    // DELETE /docas/{id}: exclui a doca, se ela não tiver agendamentos.
    @Transactional
    public void excluir(Long id) {
        // 404 se a doca não existir.
        Doca doca = buscarEntidade(id);
        // R4: apagar a doca deixaria agendamentos apontando para o nada.
        if (agendamentoRepository.existsByDocaId(id)) {
            throw new RegraDeNegocioException(operacoes.regraViolada("R4", "doca", "R4: a doca "
                    + doca.getIdentificacao() + " tem agendamentos e não pode ser excluída"));
        }
        // Sem agendamentos: pode apagar.
        repository.delete(doca);
        log.atInfo().addKeyValue("operacao", "excluir").addKeyValue("docaId", id)
                .log("Doca {} excluída: {}", id, doca.getIdentificacao());
        operacoes.contar("doca", "excluir");
    }

    // Busca a entidade ou lança a exceção que vira 404 -- usado por várias rotas.
    private Doca buscarEntidade(Long id) {
        // findById devolve um Optional; se estiver vazio, lança a exceção em vez de devolver null.
        return repository.findById(id).orElseThrow(() -> new DocaNaoEncontradaException(id));
    }
}
