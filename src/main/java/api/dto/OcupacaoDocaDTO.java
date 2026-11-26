// Declara o pacote dos DTOs -- as classes que formam o contrato da API.
package api.dto;

// Importa a anotação que documenta cada campo no Swagger.
import io.swagger.v3.oas.annotations.media.Schema;

// CAMADA: DTO
// RESPONSABILIDADE: a ocupação de UMA doca no período pedido -- uma linha do
//                   gráfico de barras do dashboard.
// SITUAÇÃO: implementada.
@Schema(description = "Ocupação de uma doca no período")
public record OcupacaoDocaDTO(

        @Schema(description = "Id da doca", example = "1")
        Long docaId,

        @Schema(description = "Identificação da doca", example = "D-01")
        String identificacao,

        @Schema(description = "Se a doca tem câmara fria", example = "true")
        boolean refrigerada,

        @Schema(description = "Quantos agendamentos encostam no período", example = "4")
        int agendamentos,

        @Schema(description = "Horas reservadas dentro do período", example = "9.5")
        double horasAgendadas,

        @Schema(description = "Horas agendadas / horas do período, em %", example = "19.8")
        double taxaOcupacao,

        // Câmara fria é o recurso mais disputado do CD: hora de doca refrigerada
        // gasta com carga seca é hora que uma carga refrigerada não consegue usar.
        @Schema(description = "Horas desta doca refrigerada ocupadas por carga seca (0 nas docas comuns)",
                example = "3.0")
        double horasDeCargaSecaEmDocaRefrigerada

// O corpo do record fica vazio: tudo o que ele precisa, o compilador gera.
) {
}
