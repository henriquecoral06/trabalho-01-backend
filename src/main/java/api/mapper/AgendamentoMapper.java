// Define o pacote dos mappers.
package api.mapper;

// Importa a anotação que registra a classe como bean do Spring.
import org.springframework.stereotype.Component;

// Importa o DTO de saída do agendamento.
import api.dto.AgendamentoResponseDTO;
// Importa a entidade convertida.
import api.model.Agendamento;

// CAMADA: Mapper
// RESPONSABILIDADE: converter Agendamento em AgendamentoResponseDTO, montando o
//                   fornecedor e a doca de dentro da resposta com os outros dois
//                   mappers. A entidade nova quem monta é o AgendamentoService,
//                   porque precisa do fornecedor e da doca buscados no banco.
// SITUAÇÃO: implementada.
@Component
public class AgendamentoMapper {

    // Monta o fornecedor que aparece dentro de cada agendamento.
    private final FornecedorMapper fornecedorMapper;

    // Monta a doca que aparece dentro de cada agendamento.
    private final DocaMapper docaMapper;

    // Recebe os mappers que vai usar: o Spring injeta os dois.
    public AgendamentoMapper(FornecedorMapper fornecedorMapper, DocaMapper docaMapper) {
        // Guarda as dependências recebidas nos atributos da classe.
        this.fornecedorMapper = fornecedorMapper;
        this.docaMapper = docaMapper;
    }

    // Toda resposta de agendamento.
    public AgendamentoResponseDTO toResponse(Agendamento agendamento) {
        // Entra um id, sai um objeto: fornecedor e doca voltam completos.
        return new AgendamentoResponseDTO(
                agendamento.getId(),
                fornecedorMapper.toResponse(agendamento.getFornecedor()),
                docaMapper.toResponse(agendamento.getDoca()),
                agendamento.getInicio(),
                agendamento.getFim(),
                agendamento.isCargaRefrigerada());
    }
}
