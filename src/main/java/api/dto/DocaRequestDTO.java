// Declara o pacote dos DTOs -- as classes que formam o contrato da API.
package api.dto;

// Importa a anotação que documenta cada campo no Swagger.
import io.swagger.v3.oas.annotations.media.Schema;
// Importa a validação de texto obrigatório.
import jakarta.validation.constraints.NotBlank;
// Importa a validação de campo obrigatório.
import jakarta.validation.constraints.NotNull;
// Importa a validação de tamanho máximo.
import jakarta.validation.constraints.Size;

// CAMADA: DTO
// RESPONSABILIDADE: o que o cliente envia para cadastrar uma doca. Não tem id:
//                   quem gera é o banco.
// SITUAÇÃO: implementada.
// Descreve o DTO inteiro na seção "Schemas" do Swagger.
@Schema(description = "Dados para cadastrar uma doca")
public record DocaRequestDTO(

        // Identificação obrigatória, com o mesmo limite da coluna no banco.
        @Schema(description = "Como a doca é chamada no pátio", example = "D-01")
        @NotBlank(message = "Identificação é obrigatória")
        @Size(max = 20, message = "Identificação tem no máximo 20 caracteres")
        String identificacao,

        // Boolean (e não boolean) para o @NotNull conseguir barrar o campo ausente.
        @Schema(description = "Se a doca tem câmara fria", example = "true")
        @NotNull(message = "Informe se a doca é refrigerada")
        Boolean refrigerada

// O corpo do record fica vazio: tudo o que ele precisa, o compilador gera.
) {
}
