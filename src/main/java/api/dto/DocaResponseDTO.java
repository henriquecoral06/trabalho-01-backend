// Declara o pacote dos DTOs -- as classes que formam o contrato da API.
package api.dto;

// Importa a anotação que documenta cada campo no Swagger.
import io.swagger.v3.oas.annotations.media.Schema;

// CAMADA: DTO
// RESPONSABILIDADE: o que a API devolve sobre uma doca.
// SITUAÇÃO: implementada.
// Descreve o DTO inteiro na seção "Schemas" do Swagger.
@Schema(description = "Uma doca do centro de distribuição")
public record DocaResponseDTO(

        // Gerado pelo banco no cadastro.
        @Schema(description = "Id da doca", example = "1")
        Long id,

        // A identificação cadastrada.
        @Schema(description = "Como a doca é chamada no pátio", example = "D-01")
        String identificacao,

        // Verdadeiro quando a doca tem câmara fria.
        @Schema(description = "Se a doca tem câmara fria", example = "true")
        boolean refrigerada

// O corpo do record fica vazio: tudo o que ele precisa, o compilador gera.
) {
}
