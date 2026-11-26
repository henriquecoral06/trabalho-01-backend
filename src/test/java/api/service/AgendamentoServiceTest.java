// Mesmo pacote do AgendamentoService: o teste enxerga tudo o que ele enxerga.
package api.service;

// Importa as asserções do AssertJ e os recursos do Mockito.
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Importa os tipos de data e o Optional.
import java.time.LocalDateTime;
import java.util.Optional;

// Importa o JUnit 5.
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// Importa o nível de log usado nas conferências.
import ch.qos.logback.classic.Level;
// Importa o registro de métricas em memória, sem Prometheus.
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

// Importa as classes do projeto usadas no teste.
import api.dto.AgendamentoRequestDTO;
import api.dto.AgendamentoResponseDTO;
import api.mapper.AgendamentoMapper;
import api.mapper.DocaMapper;
import api.mapper.FornecedorMapper;
import api.model.Agendamento;
import api.model.Doca;
import api.model.Fornecedor;
import api.observability.Operacoes;
import api.repository.AgendamentoRepository;
import api.repository.DocaRepository;
import api.repository.FornecedorRepository;
import api.support.CapturaDeLog;
import api.support.Fabrica;

// Carta C2: testes de unidade do AgendamentoService, o Service com as regras R1, R2 e R3.
//
// Os Repositories são trocados por mocks do Mockito: o teste não precisa de
// PostgreSQL nem de Docker, e roda no "./mvnw test" sem nenhum passo manual.
// O mapper e o Operacoes são os de verdade (não acessam nada de fora); as
// métricas vão para um SimpleMeterRegistry, que guarda tudo na memória.
//
// Cada teste confere três coisas: o resultado (ou a exceção), o que foi (ou não)
// gravado no banco, e o log/métrica que a operação deixou -- a "caixa de vidro".
class AgendamentoServiceTest {

    // Os dublês dos Repositories.
    private AgendamentoRepository repository;
    private FornecedorRepository fornecedorRepository;
    private DocaRepository docaRepository;

    // Métricas em memória, para conferir os contadores.
    private SimpleMeterRegistry metricas;

    // O Service testado, montado à mão (sem Spring).
    private AgendamentoService service;

    // Dados prontos usados em vários testes.
    private Fornecedor laticinios;
    private Doca docaFria;
    private Doca docaComum;

    // Um horário que passa na R3: daqui a 3 dias, das 8h às 10h.
    private final LocalDateTime inicio = LocalDateTime.now().plusDays(3).withHour(8).withMinute(0).withSecond(0)
            .withNano(0);
    private final LocalDateTime fim = inicio.plusHours(2);

    // Roda antes de cada teste: tudo novo, nenhum teste interfere no outro.
    @BeforeEach
    void preparar() {
        repository = mock(AgendamentoRepository.class);
        fornecedorRepository = mock(FornecedorRepository.class);
        docaRepository = mock(DocaRepository.class);
        metricas = new SimpleMeterRegistry();
        service = new AgendamentoService(repository, fornecedorRepository, docaRepository,
                new AgendamentoMapper(new FornecedorMapper(), new DocaMapper()), new Operacoes(metricas));

        laticinios = Fabrica.fornecedor(1, "Laticínios Serra Azul");
        docaFria = Fabrica.doca(1, "D-01", true);
        docaComum = Fabrica.doca(2, "D-02", false);
        when(fornecedorRepository.findById(1L)).thenReturn(Optional.of(laticinios));
        when(docaRepository.findById(1L)).thenReturn(Optional.of(docaFria));
        when(docaRepository.findById(2L)).thenReturn(Optional.of(docaComum));
        // save devolve a entidade com id 7, como o banco faria.
        when(repository.save(any(Agendamento.class))).thenAnswer(chamada -> {
            Agendamento a = chamada.getArgument(0);
            return Fabrica.agendamento(7, a.getFornecedor(), a.getDoca(), a.getInicio(), a.getFim(),
                    a.isCargaRefrigerada());
        });
    }

    // Monta o DTO de entrada.
    private AgendamentoRequestDTO pedido(long docaId, LocalDateTime de, LocalDateTime ate, boolean refrigerada) {
        return new AgendamentoRequestDTO(1L, docaId, de, ate, refrigerada);
    }

    // Quantas vezes a regra barrou um pedido, segundo a métrica.
    private double violacoes(String regra) {
        return metricas.counter("agenda.regras.violadas", "regra", regra).count();
    }

    @Nested
    @DisplayName("criar")
    class Criar {

        @Test
        @DisplayName("sucesso: carga refrigerada em doca refrigerada é gravada, com log INFO e métrica")
        void criaAgendamento() {
            try (CapturaDeLog logs = CapturaDeLog.de(AgendamentoService.class)) {
                AgendamentoResponseDTO criado = service.criar(pedido(1, inicio, fim, true));

                assertThat(criado.id()).isEqualTo(7L);
                assertThat(criado.doca().identificacao()).isEqualTo("D-01");
                assertThat(criado.fornecedor().nome()).isEqualTo("Laticínios Serra Azul");
                verify(repository).save(any(Agendamento.class));
                // Caixa de vidro: a operação deixou rastro no log, com os campos estruturados.
                assertThat(logs.tem(Level.INFO, "Agendamento 7 criado")).isTrue();
                assertThat(logs.temCampo("operacao", "criar")).isTrue();
                assertThat(logs.temCampo("docaId", 1)).isTrue();
            }
            assertThat(metricas.counter("agenda.operacoes", "entidade", "agendamento", "operacao", "criar").count())
                    .isEqualTo(1.0);
        }

        @Test
        @DisplayName("sucesso: carga seca pode ir para doca refrigerada (a R2 só barra o contrário)")
        void cargaSecaEmDocaFria() {
            assertThat(service.criar(pedido(1, inicio, fim, false)).cargaRefrigerada()).isFalse();
        }

        @Test
        @DisplayName("R1: horário sobreposto na mesma doca é recusado, nada é gravado e sai WARN")
        void r1HorarioSobreposto() {
            Agendamento existente = Fabrica.agendamento(3, laticinios, docaFria, inicio.plusHours(1),
                    fim.plusHours(1), false);
            when(repository.findFirstByDocaIdAndInicioBeforeAndFimAfter(1L, fim, inicio))
                    .thenReturn(Optional.of(existente));

            try (CapturaDeLog logs = CapturaDeLog.de(Operacoes.class)) {
                assertThatThrownBy(() -> service.criar(pedido(1, inicio, fim, false)))
                        .isInstanceOf(RegraDeNegocioException.class)
                        .hasMessageStartingWith("R1: a doca D-01 já tem um agendamento");
                assertThat(logs.tem(Level.WARN, "R1")).isTrue();
                assertThat(logs.temCampo("regra", "R1")).isTrue();
            }
            verify(repository, never()).save(any());
            assertThat(violacoes("R1")).isEqualTo(1.0);
        }

        @Test
        @DisplayName("R2: carga refrigerada em doca comum é recusada")
        void r2CargaRefrigeradaEmDocaComum() {
            assertThatThrownBy(() -> service.criar(pedido(2, inicio, fim, true)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessage("R2: a carga é refrigerada e a doca D-02 não é refrigerada");
            verify(repository, never()).save(any());
            assertThat(violacoes("R2")).isEqualTo(1.0);
        }

        @Test
        @DisplayName("R3: agendar para daqui a 2 horas é recusado (mínimo de 24 horas)")
        void r3EmCimaDaHora() {
            LocalDateTime daquiADuasHoras = LocalDateTime.now().plusHours(2);

            assertThatThrownBy(() -> service.criar(pedido(1, daquiADuasHoras, daquiADuasHoras.plusHours(1), false)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("R3")
                    .hasMessageContaining("24 horas");
            verify(repository, never()).save(any());
            assertThat(violacoes("R3")).isEqualTo(1.0);
        }

        @Test
        @DisplayName("fim antes do início é recusado antes de qualquer regra")
        void fimAntesDoInicio() {
            assertThatThrownBy(() -> service.criar(pedido(1, fim, inicio, false)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessage("O fim do agendamento precisa ser depois do início");
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("fornecedor inexistente: FornecedorNaoEncontradoException (404)")
        void fornecedorInexistente() {
            when(fornecedorRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.criar(pedido(1, inicio, fim, false)))
                    .isInstanceOf(FornecedorNaoEncontradoException.class)
                    .hasMessage("Fornecedor não encontrado: 1");
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("doca inexistente: DocaNaoEncontradaException (404)")
        void docaInexistente() {
            assertThatThrownBy(() -> service.criar(pedido(99, inicio, fim, false)))
                    .isInstanceOf(DocaNaoEncontradaException.class)
                    .hasMessage("Doca não encontrada: 99");
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("atualizar (remarcar)")
    class Atualizar {

        @Test
        @DisplayName("sucesso: na R1, o próprio agendamento não conta como conflito; log INFO 'remarcado'")
        void remarca() {
            Agendamento atual = Fabrica.agendamento(7, laticinios, docaFria, inicio, fim, false);
            when(repository.findById(7L)).thenReturn(Optional.of(atual));
            when(repository.save(atual)).thenReturn(atual);

            try (CapturaDeLog logs = CapturaDeLog.de(AgendamentoService.class)) {
                AgendamentoResponseDTO remarcado = service.atualizar(7L, pedido(1, inicio.plusHours(1),
                        fim.plusHours(1), true));

                assertThat(remarcado.inicio()).isEqualTo(inicio.plusHours(1));
                assertThat(remarcado.cargaRefrigerada()).isTrue();
                assertThat(logs.tem(Level.INFO, "Agendamento 7 remarcado")).isTrue();
            }
            // A consulta da R1 usada foi a que ignora o id 7.
            verify(repository).findFirstByDocaIdAndIdNotAndInicioBeforeAndFimAfter(1L, 7L, fim.plusHours(1),
                    inicio.plusHours(1));
            verify(repository, never()).findFirstByDocaIdAndInicioBeforeAndFimAfter(anyLong(), any(), any());
        }

        @Test
        @DisplayName("R2 vale na remarcação: mudar para doca comum com carga refrigerada é recusado")
        void r2NaRemarcacao() {
            Agendamento atual = Fabrica.agendamento(7, laticinios, docaFria, inicio, fim, true);
            when(repository.findById(7L)).thenReturn(Optional.of(atual));

            assertThatThrownBy(() -> service.atualizar(7L, pedido(2, inicio, fim, true)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageStartingWith("R2");
            verify(repository, never()).save(any());
            // A entidade não foi alterada.
            assertThat(atual.getDoca()).isSameAs(docaFria);
        }

        @Test
        @DisplayName("agendamento inexistente: AgendamentoNaoEncontradoException (404)")
        void inexistente() {
            when(repository.findById(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.atualizar(5L, pedido(1, inicio, fim, false)))
                    .isInstanceOf(AgendamentoNaoEncontradoException.class);
        }
    }

    @Nested
    @DisplayName("excluir (cancelar) e buscar")
    class ExcluirEBuscar {

        @Test
        @DisplayName("excluir: apaga, registra log INFO 'cancelado' e soma na métrica")
        void excluiAgendamento() {
            Agendamento atual = Fabrica.agendamento(7, laticinios, docaFria, inicio, fim, false);
            when(repository.findById(7L)).thenReturn(Optional.of(atual));

            try (CapturaDeLog logs = CapturaDeLog.de(AgendamentoService.class)) {
                service.excluir(7L);
                assertThat(logs.tem(Level.INFO, "Agendamento 7 cancelado")).isTrue();
            }
            verify(repository).delete(atual);
            assertThat(metricas.counter("agenda.operacoes", "entidade", "agendamento", "operacao", "excluir").count())
                    .isEqualTo(1.0);
        }

        @Test
        @DisplayName("excluir inexistente: 404 e nada é apagado")
        void excluirInexistente() {
            when(repository.findById(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.excluir(5L)).isInstanceOf(AgendamentoNaoEncontradoException.class);
            verify(repository, never()).delete(any(Agendamento.class));
        }

        @Test
        @DisplayName("buscar por id: devolve o DTO com fornecedor e doca completos")
        void buscaPorId() {
            when(repository.findById(7L)).thenReturn(Optional.of(
                    Fabrica.agendamento(7, laticinios, docaFria, inicio, fim, true)));

            AgendamentoResponseDTO dto = service.buscarPorId(7L);

            assertThat(dto.fornecedor().id()).isEqualTo(1L);
            assertThat(dto.doca().refrigerada()).isTrue();
        }
    }
}
