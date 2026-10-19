// Declara o pacote dos DTOs -- as classes que formam o contrato da API.
package api.dto;

// Importa o tipo de data e hora do início e do fim.
import java.time.LocalDateTime;

// Importa a anotação que documenta cada campo no Swagger.
import io.swagger.v3.oas.annotations.media.Schema;

// CAMADA: DTO
// RESPONSABILIDADE: o que a API devolve sobre um agendamento. Entra um id, sai
//                   um objeto: fornecedor e doca voltam completos.
// SITUAÇÃO: implementada.
// Descreve o DTO inteiro na seção "Schemas" do Swagger.
@Schema(description = "Um agendamento de descarga")
public record AgendamentoResponseDTO(

        // Gerado pelo banco no registro.
        @Schema(description = "Id do agendamento", example = "1")
        Long id,

        // Quem vai entregar.
        @Schema(description = "O fornecedor do agendamento")
        FornecedorResponseDTO fornecedor,

        // Em qual doca.
        @Schema(description = "A doca reservada")
        DocaResponseDTO doca,

        // Quando a doca começa a ficar reservada.
        @Schema(description = "Início da descarga", example = "2026-10-27T08:00:00")
        LocalDateTime inicio,

        // Quando a doca fica livre de novo.
        @Schema(description = "Fim da descarga", example = "2026-10-27T09:00:00")
        LocalDateTime fim,

        // Verdadeiro quando a carga precisa de câmara fria.
        @Schema(description = "Se a carga precisa de câmara fria", example = "true")
        boolean cargaRefrigerada

// O corpo do record fica vazio: tudo o que ele precisa, o compilador gera.
) {
}
