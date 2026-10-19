// Define o pacote da camada de regras de negócio.
package api.service;

// Importa a lista devolvida na listagem.
import java.util.List;

// Importa a anotação que registra a classe como serviço Spring.
import org.springframework.stereotype.Service;

// Importa os DTOs de entrada e de saída da doca.
import api.dto.DocaRequestDTO;
import api.dto.DocaResponseDTO;
// Importa o mapper que traduz DTO <-> entidade.
import api.mapper.DocaMapper;
// Importa a entidade persistida.
import api.model.Doca;
// Importa o repositório usado pelo service.
import api.repository.DocaRepository;

// CAMADA: Service
// RESPONSABILIDADE: cadastrar, listar e buscar docas. Recebe DTO, usa o mapper
//                   e devolve DTO: a entidade Doca não sobe para o Controller.
// REGRAS: nenhuma específica de doca. As regras que usam a doca (R1 e R2)
//         moram no AgendamentoService.
// ROTAS ATENDIDAS: POST /docas, GET /docas e GET /docas/{id}.
// SITUAÇÃO: implementada.
// Registra a classe como bean da camada de serviço.
@Service
public class DocaService {

    // Acesso à tabela docas.
    private final DocaRepository repository;

    // Converte DTO <-> entidade.
    private final DocaMapper mapper;

    // Recebe as dependências que o Spring injeta automaticamente.
    public DocaService(DocaRepository repository, DocaMapper mapper) {
        // Guarda as dependências recebidas nos atributos da classe.
        this.repository = repository;
        this.mapper = mapper;
    }

    // POST /docas: monta a entidade, grava e devolve a doca com o id gerado.
    public DocaResponseDTO criar(DocaRequestDTO dto) {
        // O mapper monta a doca nova, ainda sem id.
        Doca doca = mapper.toEntity(dto);
        // O banco gera o id no INSERT e devolve a entidade preenchida.
        Doca salva = repository.save(doca);
        // A entidade para aqui: para cima vai só o DTO.
        return mapper.toResponse(salva);
    }

    // GET /docas: devolve todas as docas. Sem docas, a lista vem vazia.
    public List<DocaResponseDTO> listarTodas() {
        // Busca todas e converte a lista inteira.
        return mapper.toResponseList(repository.findAll());
    }

    // GET /docas/{id}: devolve uma doca ou lança a exceção que vira 404.
    public DocaResponseDTO buscarPorId(Long id) {
        // findById devolve um Optional; se estiver vazio, lança a exceção em vez de devolver null.
        Doca doca = repository.findById(id).orElseThrow(() -> new DocaNaoEncontradaException(id));
        // Converte a doca encontrada.
        return mapper.toResponse(doca);
    }
}
