// Define o pacote dos mappers.
package api.mapper;

// Importa a lista usada na conversão em lote.
import java.util.List;

// Importa a anotação que registra a classe como bean do Spring.
import org.springframework.stereotype.Component;

// Importa os DTOs de entrada e de saída da doca.
import api.dto.DocaRequestDTO;
import api.dto.DocaResponseDTO;
// Importa a entidade convertida.
import api.model.Doca;

// CAMADA: Mapper
// RESPONSABILIDADE: converter DocaRequestDTO em Doca, e Doca em DocaResponseDTO.
//                   Também é usado pelo AgendamentoMapper, para montar a doca
//                   que aparece dentro de cada agendamento.
// SITUAÇÃO: implementada.
// @Component: o Spring cria um DocaMapper e injeta no DocaService.
@Component
public class DocaMapper {

    // POST: monta uma doca nova a partir do DTO (id = null, o banco gera).
    public Doca toEntity(DocaRequestDTO dto) {
        // Usa o construtor de cadastro da entidade.
        return new Doca(dto.identificacao(), dto.refrigerada());
    }

    // Toda resposta: copia da entidade só o que o contrato da API mostra.
    public DocaResponseDTO toResponse(Doca doca) {
        // Monta o record com os três campos de saída.
        return new DocaResponseDTO(doca.getId(), doca.getIdentificacao(), doca.isRefrigerada());
    }

    // GET de listagem: converte cada doca da lista.
    public List<DocaResponseDTO> toResponseList(List<Doca> docas) {
        // Reaproveita o toResponse para cada item.
        return docas.stream().map(this::toResponse).toList();
    }
}
