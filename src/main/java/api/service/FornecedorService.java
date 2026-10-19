// Define o pacote da camada de regras de negócio.
package api.service;

// Importa a anotação que registra a classe como serviço Spring.
import org.springframework.stereotype.Service;

// Importa os DTOs de entrada e de saída do fornecedor.
import api.dto.FornecedorRequestDTO;
import api.dto.FornecedorResponseDTO;
// Importa o mapper e o repositório que o service usa.
import api.mapper.FornecedorMapper;
import api.repository.FornecedorRepository;

// CAMADA: Service
// RESPONSABILIDADE: cadastrar fornecedores.
// REGRAS: nenhuma específica de fornecedor. As regras do agendamento (R1, R2
//         e R3) moram no AgendamentoService.
// ROTAS ATENDIDAS: POST /fornecedores. GET /fornecedores e GET /fornecedores/{id}
//                  estão no contrato e entram na próxima etapa.
// SITUAÇÃO: implementada em parte. Só o cadastro, que as regras precisam: sem
//           fornecedor cadastrado, não há agendamento.
@Service
public class FornecedorService {

    // Acesso à tabela fornecedores.
    private final FornecedorRepository repository;

    // Converte DTO <-> entidade.
    private final FornecedorMapper mapper;

    // Recebe as dependências que o Spring injeta automaticamente.
    public FornecedorService(FornecedorRepository repository, FornecedorMapper mapper) {
        // Guarda as dependências recebidas nos atributos da classe.
        this.repository = repository;
        this.mapper = mapper;
    }

    // POST /fornecedores: monta a entidade, grava e devolve o fornecedor com o id gerado.
    public FornecedorResponseDTO criar(FornecedorRequestDTO dto) {
        // Grava o fornecedor novo e converte o resultado, já com o id.
        return mapper.toResponse(repository.save(mapper.toEntity(dto)));
    }
}
