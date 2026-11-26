// Declara o pacote dos DTOs -- as classes que formam o contrato da API.
package api.dto;

// Importa o tipo de data do período.
import java.time.LocalDate;
// Importa a lista de docas e de alertas.
import java.util.List;

// Importa a anotação que documenta cada campo no Swagger.
import io.swagger.v3.oas.annotations.media.Schema;

// CAMADA: DTO
// RESPONSABILIDADE: o resultado de GET /docas/ocupacao, pronto para alimentar
//                   um dashboard: o período, a taxa geral, uma linha por doca
//                   (da mais ocupada para a menos) e os alertas para o coordenador.
// SITUAÇÃO: implementada.
@Schema(description = "Ocupação das docas num período, para alimentar um dashboard")
public record OcupacaoDTO(

        @Schema(description = "Primeiro dia do período", example = "2026-12-01")
        LocalDate de,

        @Schema(description = "Último dia do período (inclusive)", example = "2026-12-02")
        LocalDate ate,

        @Schema(description = "Horas que cada doca tem no período (24 h por dia)", example = "48.0")
        double horasDisponiveisPorDoca,

        @Schema(description = "Horas agendadas de todas as docas / horas disponíveis de todas, em %",
                example = "12.5")
        double taxaOcupacaoGeral,

        @Schema(description = "Uma linha por doca, da mais ocupada para a menos ocupada")
        List<OcupacaoDocaDTO> docas,

        @Schema(description = "Situações que o coordenador precisa olhar",
                example = "[\"A doca D-01 (refrigerada) está 87.5% ocupada: perto do limite\"]")
        List<String> alertas

// O corpo do record fica vazio: tudo o que ele precisa, o compilador gera.
) {
}
