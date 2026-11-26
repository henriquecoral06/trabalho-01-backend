// Mesmo pacote do OcupacaoService.
package api.service;

// Importa as asserções do AssertJ e os recursos do Mockito.
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Importa os tipos de data e a lista.
import java.time.LocalDate;
import java.util.List;

// Importa o JUnit 5.
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
// Importa a ordenação usada na busca das docas.
import org.springframework.data.domain.Sort;

// Importa as classes do projeto usadas no teste.
import api.dto.OcupacaoDTO;
import api.dto.OcupacaoDocaDTO;
import api.model.Doca;
import api.model.Fornecedor;
import api.repository.AgendamentoRepository;
import api.repository.DocaRepository;
import api.support.Fabrica;

// Testes da aplicação prática (Visualização de Dados): o cálculo da ocupação das docas.
class OcupacaoServiceTest {

    private DocaRepository docaRepository;
    private AgendamentoRepository agendamentoRepository;
    private OcupacaoService service;

    private final LocalDate dia = LocalDate.of(2026, 12, 1);
    private final Fornecedor fornecedor = Fabrica.fornecedor(1, "Laticínios Serra Azul");
    private final Doca fria = Fabrica.doca(1, "D-01", true);
    private final Doca comum = Fabrica.doca(2, "D-02", false);

    @BeforeEach
    void preparar() {
        docaRepository = mock(DocaRepository.class);
        agendamentoRepository = mock(AgendamentoRepository.class);
        service = new OcupacaoService(docaRepository, agendamentoRepository);
        when(docaRepository.findAll(any(Sort.class))).thenReturn(List.of(fria, comum));
    }

    @Test
    @DisplayName("calcula horas e taxa por doca, cortando o que passa da borda do período")
    void calculaOcupacao() {
        when(agendamentoRepository.findByInicioBeforeAndFimAfter(any(), any())).thenReturn(List.of(
                // 6 h dentro do dia, carga refrigerada.
                Fabrica.agendamento(1, fornecedor, fria, dia.atTime(8, 0), dia.atTime(14, 0), true),
                // Das 22h às 02h do dia seguinte: só 2 h caem dentro do período.
                Fabrica.agendamento(2, fornecedor, fria, dia.atTime(22, 0), dia.plusDays(1).atTime(2, 0), false)));

        OcupacaoDTO ocupacao = service.calcular(dia, dia);

        assertThat(ocupacao.horasDisponiveisPorDoca()).isEqualTo(24.0);
        OcupacaoDocaDTO d01 = ocupacao.docas().getFirst();
        assertThat(d01.identificacao()).isEqualTo("D-01");
        assertThat(d01.agendamentos()).isEqualTo(2);
        assertThat(d01.horasAgendadas()).isEqualTo(8.0);
        // 8 h de 24 h = 33,3%.
        assertThat(d01.taxaOcupacao()).isEqualTo(33.3);
        // A carga seca ocupou 2 h da câmara fria.
        assertThat(d01.horasDeCargaSecaEmDocaRefrigerada()).isEqualTo(2.0);
        // A doca parada também aparece, com 0%.
        assertThat(ocupacao.docas().get(1).taxaOcupacao()).isZero();
        // 8 h de 48 h (duas docas) = 16,7%.
        assertThat(ocupacao.taxaOcupacaoGeral()).isEqualTo(16.7);
        assertThat(ocupacao.alertas()).singleElement().asString().contains("D-01").contains("carga seca");
    }

    @Test
    @DisplayName("doca com 85% ou mais de ocupação gera alerta de limite")
    void alertaDeLimite() {
        when(agendamentoRepository.findByInicioBeforeAndFimAfter(any(), any())).thenReturn(List.of(
                Fabrica.agendamento(1, fornecedor, comum, dia.atTime(1, 0), dia.atTime(23, 0), false)));

        OcupacaoDTO ocupacao = service.calcular(dia, dia);

        assertThat(ocupacao.docas().getFirst().identificacao()).isEqualTo("D-02");
        assertThat(ocupacao.alertas()).anyMatch(a -> a.contains("D-02") && a.contains("perto do limite"));
    }

    @Test
    @DisplayName("período com fim antes do início é recusado sem consultar o banco")
    void periodoInvertido() {
        assertThatThrownBy(() -> service.calcular(dia, dia.minusDays(1)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("antes do início");
        verifyNoInteractions(agendamentoRepository);
    }

    @Test
    @DisplayName("período de mais de 31 dias é recusado")
    void periodoLongoDemais() {
        assertThatThrownBy(() -> service.calcular(dia, dia.plusDays(31)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("O período pode ter no máximo 31 dias");
    }
}
