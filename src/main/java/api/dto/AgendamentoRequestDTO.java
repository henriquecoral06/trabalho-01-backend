// Declara o pacote dos DTOs -- as classes que formam o contrato da API.
package api.dto;

// Importa o tipo de data e hora do início e do fim.
import java.time.LocalDateTime;

// Importa a anotação que documenta cada campo no Swagger.
import io.swagger.v3.oas.annotations.media.Schema;
// Importa a validação de campo obrigatório.
import jakarta.validation.constraints.NotNull;

// CAMADA: DTO
// RESPONSABILIDADE: o que o cliente envia para agendar uma descarga. Entram só
//                   os ids: nome do fornecedor e dados da doca já estão no banco.
//                   Aqui só se confere o formato; as regras R1, R2 e R3 quem
//                   confere é o AgendamentoService.
// SITUAÇÃO: implementada.
// Descreve o DTO inteiro na seção "Schemas" do Swagger.
@Schema(description = "Dados para agendar a descarga de um fornecedor numa doca")
public record AgendamentoRequestDTO(

        // Quem vai entregar.
        @Schema(description = "Id do fornecedor", example = "1")
        @NotNull(message = "O id do fornecedor é obrigatório")
        Long fornecedorId,

        // Em qual doca.
        @Schema(description = "Id da doca", example = "1")
        @NotNull(message = "O id da doca é obrigatório")
        Long docaId,

        // Quando a doca começa a ficar reservada.
        @Schema(description = "Início da descarga", example = "2026-10-27T08:00:00")
        @NotNull(message = "O início é obrigatório")
        LocalDateTime inicio,

        // Quando a doca fica livre de novo.
        @Schema(description = "Fim da descarga", example = "2026-10-27T09:00:00")
        @NotNull(message = "O fim é obrigatório")
        LocalDateTime fim,

        // Boolean (e não boolean) para o @NotNull conseguir barrar o campo ausente.
        @Schema(description = "Se a carga precisa de câmara fria", example = "true")
        @NotNull(message = "Informe se a carga é refrigerada")
        Boolean cargaRefrigerada

// O corpo do record fica vazio: tudo o que ele precisa, o compilador gera.
) {
}
