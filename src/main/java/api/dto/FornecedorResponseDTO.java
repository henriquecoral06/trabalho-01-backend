// Declara o pacote dos DTOs -- as classes que formam o contrato da API.
package api.dto;

// Importa a anotação que documenta cada campo no Swagger.
import io.swagger.v3.oas.annotations.media.Schema;

// CAMADA: DTO
// RESPONSABILIDADE: o que a API devolve sobre um fornecedor.
// SITUAÇÃO: implementada.
// Descreve o DTO inteiro na seção "Schemas" do Swagger.
@Schema(description = "Um fornecedor do centro de distribuição")
public record FornecedorResponseDTO(

        // Gerado pelo banco no cadastro.
        @Schema(description = "Id do fornecedor", example = "1")
        Long id,

        // O nome cadastrado.
        @Schema(description = "Nome do fornecedor", example = "Laticínios Serra Azul")
        String nome,

        // O CNPJ cadastrado.
        @Schema(description = "CNPJ do fornecedor", example = "12.345.678/0001-90")
        String cnpj

// O corpo do record fica vazio: tudo o que ele precisa, o compilador gera.
) {
}
