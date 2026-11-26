// Define o pacote da camada de regras de negócio.
package api.service;

// Importa os tipos de paginação do Spring Data.
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
// Importa o tipo que junta os filtros opcionais num WHERE só.
import org.springframework.data.jpa.domain.Specification;
// Importa a anotação que registra a classe como serviço Spring.
import org.springframework.stereotype.Service;
// Importa a anotação que faz o método inteiro valer como uma operação só no banco.
import org.springframework.transaction.annotation.Transactional;

// Importa os DTOs de entrada e de saída do fornecedor.
import api.dto.FornecedorRequestDTO;
import api.dto.FornecedorResponseDTO;
// Importa o mapper que traduz DTO <-> entidade.
import api.mapper.FornecedorMapper;
// Importa a entidade persistida.
import api.model.Fornecedor;
// Importa os repositórios usados pelo service.
import api.repository.AgendamentoRepository;
import api.repository.FornecedorRepository;
// Importa os filtros opcionais da listagem.
import api.repository.Filtros;

// CAMADA: Service
// RESPONSABILIDADE: CRUD dos fornecedores.
// REGRAS: R4 (fornecedor com agendamentos não pode ser excluído). As regras do
//         agendamento (R1, R2 e R3) moram no AgendamentoService.
// ROTAS ATENDIDAS: POST, GET (paginado, com filtros), GET /{id}, PUT /{id} e DELETE /{id} de /fornecedores.
// SITUAÇÃO: implementada.
@Service
public class FornecedorService {

    // Acesso à tabela fornecedores.
    private final FornecedorRepository repository;

    // Usado pela R4, para saber se o fornecedor tem agendamentos.
    private final AgendamentoRepository agendamentoRepository;

    // Converte DTO <-> entidade.
    private final FornecedorMapper mapper;

    // Recebe as dependências que o Spring injeta automaticamente.
    public FornecedorService(FornecedorRepository repository, AgendamentoRepository agendamentoRepository,
                             FornecedorMapper mapper) {
        // Guarda as dependências recebidas nos atributos da classe.
        this.repository = repository;
        this.agendamentoRepository = agendamentoRepository;
        this.mapper = mapper;
    }

    // POST /fornecedores: monta a entidade, grava e devolve o fornecedor com o id gerado.
    public FornecedorResponseDTO criar(FornecedorRequestDTO dto) {
        // Grava o fornecedor novo; o banco gera o id.
        Fornecedor salvo = repository.save(mapper.toEntity(dto));
        // Converte o resultado, já com o id.
        return mapper.toResponse(salvo);
    }

    // GET /fornecedores: uma página de fornecedores, com os filtros opcionais.
    public Page<FornecedorResponseDTO> listar(String nome, String cnpj, Pageable pageable) {
        // Junta os filtros: os que vieram null são ignorados.
        Specification<Fornecedor> filtro = Specification.allOf(
                Filtros.fornecedorNomeContem(nome),
                Filtros.fornecedorCnpjComeca(cnpj));
        // Page.map converte cada item e mantém os dados da paginação.
        return repository.findAll(filtro, pageable).map(mapper::toResponse);
    }

    // GET /fornecedores/{id}: devolve um fornecedor ou lança a exceção que vira 404.
    public FornecedorResponseDTO buscarPorId(Long id) {
        // Converte o fornecedor encontrado.
        return mapper.toResponse(buscarEntidade(id));
    }

    // PUT /fornecedores/{id}: troca os dados do fornecedor.
    @Transactional
    public FornecedorResponseDTO atualizar(Long id, FornecedorRequestDTO dto) {
        // 404 se o fornecedor não existir.
        Fornecedor fornecedor = buscarEntidade(id);
        // Copia os dados novos para a entidade.
        mapper.updateEntity(fornecedor, dto);
        // Grava e devolve o fornecedor atualizado.
        return mapper.toResponse(repository.save(fornecedor));
    }

    // DELETE /fornecedores/{id}: exclui o fornecedor, se ele não tiver agendamentos.
    @Transactional
    public void excluir(Long id) {
        // 404 se o fornecedor não existir.
        Fornecedor fornecedor = buscarEntidade(id);
        // R4: apagar o fornecedor deixaria agendamentos sem dono.
        if (agendamentoRepository.existsByFornecedorId(id)) {
            throw new RegraDeNegocioException("R4: o fornecedor " + fornecedor.getNome()
                    + " tem agendamentos e não pode ser excluído");
        }
        // Sem agendamentos: pode apagar.
        repository.delete(fornecedor);
    }

    // Busca a entidade ou lança a exceção que vira 404 -- usado por várias rotas.
    private Fornecedor buscarEntidade(Long id) {
        // findById devolve um Optional; se estiver vazio, lança a exceção.
        return repository.findById(id).orElseThrow(() -> new FornecedorNaoEncontradoException(id));
    }
}
