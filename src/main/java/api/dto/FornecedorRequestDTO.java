// Declara o pacote dos DTOs -- as classes que formam o contrato da API.
package api.dto;

// Importa a anotação que documenta cada campo no Swagger.
import io.swagger.v3.oas.annotations.media.Schema;
// Importa a validação de texto obrigatório.
import jakarta.validation.constraints.NotBlank;
// Importa a validação de tamanho máximo.
import jakarta.validation.constraints.Size;

// CAMADA: DTO
// RESPONSABILIDADE: o que o cliente envia para cadastrar um fornecedor. Não tem
//                   id: quem gera é o banco.
// SITUAÇÃO: implementada.
// Descreve o DTO inteiro na seção "Schemas" do Swagger.
@Schema(description = "Dados para cadastrar um fornecedor")
public record FornecedorRequestDTO(

        // Nome obrigatório.
        @Schema(description = "Nome do fornecedor", example = "Laticínios Serra Azul")
        @NotBlank(message = "Nome é obrigatório")
        String nome,

        // CNPJ obrigatório, com o mesmo limite da coluna no banco.
        @Schema(description = "CNPJ do fornecedor", example = "12.345.678/0001-90")
        @NotBlank(message = "CNPJ é obrigatório")
        @Size(max = 18, message = "CNPJ tem no máximo 18 caracteres")
        String cnpj

// O corpo do record fica vazio: tudo o que ele precisa, o compilador gera.
) {
}
