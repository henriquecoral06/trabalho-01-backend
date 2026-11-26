// Mesmo pacote do DocaService.
package api.service;

// Importa as asserções do AssertJ e os recursos do Mockito.
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Importa os tipos usados nos dublês.
import java.util.List;
import java.util.Optional;

// Importa o JUnit 5.
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
// Importa os tipos de paginação do Spring Data.
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

// Importa o nível de log e o registro de métricas em memória.
import ch.qos.logback.classic.Level;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

// Importa as classes do projeto usadas no teste.
import api.dto.DocaRequestDTO;
import api.dto.DocaResponseDTO;
import api.mapper.DocaMapper;
import api.model.Doca;
import api.observability.Operacoes;
import api.repository.AgendamentoRepository;
import api.repository.DocaRepository;
import api.support.CapturaDeLog;
import api.support.Fabrica;

// Carta C2: testes de unidade do DocaService -- CRUD, regra R4 e os logs de cada operação.
class DocaServiceTest {

    private DocaRepository repository;
    private AgendamentoRepository agendamentoRepository;
    private SimpleMeterRegistry metricas;
    private DocaService service;

    @BeforeEach
    void preparar() {
        repository = mock(DocaRepository.class);
        agendamentoRepository = mock(AgendamentoRepository.class);
        metricas = new SimpleMeterRegistry();
        service = new DocaService(repository, agendamentoRepository, new DocaMapper(), new Operacoes(metricas));
    }

    @Test
    @DisplayName("criar: grava, devolve o id gerado e registra log INFO")
    void cria() {
        when(repository.save(any(Doca.class))).thenReturn(Fabrica.doca(1, "D-01", true));

        try (CapturaDeLog logs = CapturaDeLog.de(DocaService.class)) {
            DocaResponseDTO criada = service.criar(new DocaRequestDTO("D-01", true));

            assertThat(criada.id()).isEqualTo(1L);
            assertThat(logs.tem(Level.INFO, "Doca 1 criada: D-01")).isTrue();
        }
        assertThat(metricas.counter("agenda.operacoes", "entidade", "doca", "operacao", "criar").count())
                .isEqualTo(1.0);
    }

    @Test
    @DisplayName("listar: converte a página do repositório mantendo a paginação")
    @SuppressWarnings("unchecked")
    void lista() {
        PageRequest pagina = PageRequest.of(0, 10);
        when(repository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(Fabrica.doca(1, "D-01", true)), pagina, 1));

        Page<DocaResponseDTO> resultado = service.listar("d-0", true, pagina);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().getFirst().identificacao()).isEqualTo("D-01");
    }

    @Test
    @DisplayName("atualizar: troca os dados e o log mostra o antes e o depois")
    void atualiza() {
        Doca doca = Fabrica.doca(1, "D-01", false);
        when(repository.findById(1L)).thenReturn(Optional.of(doca));
        when(repository.save(doca)).thenReturn(doca);

        try (CapturaDeLog logs = CapturaDeLog.de(DocaService.class)) {
            DocaResponseDTO alterada = service.atualizar(1L, new DocaRequestDTO("D-01A", true));

            assertThat(alterada.identificacao()).isEqualTo("D-01A");
            assertThat(alterada.refrigerada()).isTrue();
            assertThat(logs.tem(Level.INFO, "Doca 1 alterada: D-01 (refrigerada: false) -> D-01A")).isTrue();
        }
    }

    @Test
    @DisplayName("atualizar inexistente: DocaNaoEncontradaException e nada é gravado")
    void atualizaInexistente() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(9L, new DocaRequestDTO("X", false)))
                .isInstanceOf(DocaNaoEncontradaException.class)
                .hasMessage("Doca não encontrada: 9");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("excluir sem agendamentos: apaga e registra log INFO")
    void exclui() {
        Doca doca = Fabrica.doca(1, "D-01", true);
        when(repository.findById(1L)).thenReturn(Optional.of(doca));
        when(agendamentoRepository.existsByDocaId(1L)).thenReturn(false);

        try (CapturaDeLog logs = CapturaDeLog.de(DocaService.class)) {
            service.excluir(1L);
            assertThat(logs.tem(Level.INFO, "Doca 1 excluída")).isTrue();
        }
        verify(repository).delete(doca);
    }

    @Test
    @DisplayName("R4: doca com agendamentos não é excluída, sai WARN e a métrica da regra sobe")
    void r4DocaComAgendamentos() {
        when(repository.findById(1L)).thenReturn(Optional.of(Fabrica.doca(1, "D-01", true)));
        when(agendamentoRepository.existsByDocaId(1L)).thenReturn(true);

        try (CapturaDeLog logs = CapturaDeLog.de(Operacoes.class)) {
            assertThatThrownBy(() -> service.excluir(1L))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessage("R4: a doca D-01 tem agendamentos e não pode ser excluída");
            assertThat(logs.tem(Level.WARN, "R4")).isTrue();
        }
        verify(repository, never()).delete(any(Doca.class));
        assertThat(metricas.counter("agenda.regras.violadas", "regra", "R4").count()).isEqualTo(1.0);
    }
}
