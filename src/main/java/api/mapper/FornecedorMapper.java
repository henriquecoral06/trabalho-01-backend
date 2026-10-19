// Define o pacote dos mappers.
package api.mapper;

// Importa a anotação que registra a classe como bean do Spring.
import org.springframework.stereotype.Component;

// Importa os DTOs de entrada e de saída do fornecedor.
import api.dto.FornecedorRequestDTO;
import api.dto.FornecedorResponseDTO;
// Importa a entidade convertida.
import api.model.Fornecedor;

// CAMADA: Mapper
// RESPONSABILIDADE: converter FornecedorRequestDTO em Fornecedor, e Fornecedor em
//                   FornecedorResponseDTO. Também é usado pelo AgendamentoMapper,
//                   para montar o fornecedor que aparece dentro de cada agendamento.
// SITUAÇÃO: implementada. Tem os métodos que POST /fornecedores e os agendamentos usam.
@Component
public class FornecedorMapper {

    // POST /fornecedores: monta um fornecedor novo a partir do DTO (id = null, o banco gera).
    public Fornecedor toEntity(FornecedorRequestDTO dto) {
        // Usa o construtor de cadastro da entidade.
        return new Fornecedor(dto.nome(), dto.cnpj());
    }

    // Toda resposta: copia da entidade só o que o contrato da API mostra.
    public FornecedorResponseDTO toResponse(Fornecedor fornecedor) {
        // Monta o record com os três campos de saída.
        return new FornecedorResponseDTO(fornecedor.getId(), fornecedor.getNome(), fornecedor.getCnpj());
    }
}
