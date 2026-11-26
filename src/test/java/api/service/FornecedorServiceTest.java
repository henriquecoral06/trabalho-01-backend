// Mesmo pacote do FornecedorService.
package api.service;

// Importa as asserções do AssertJ e os recursos do Mockito.
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Importa o Optional devolvido pelo findById.
import java.util.Optional;

// Importa o JUnit 5.
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// Importa o nível de log e o registro de métricas em memória.
import ch.qos.logback.classic.Level;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

// Importa as classes do projeto usadas no teste.
import api.dto.FornecedorRequestDTO;
import api.mapper.FornecedorMapper;
import api.model.Fornecedor;
import api.observability.Operacoes;
import api.repository.AgendamentoRepository;
import api.repository.FornecedorRepository;
import api.support.CapturaDeLog;
import api.support.Fabrica;

// Carta C2: testes de unidade do FornecedorService.
class FornecedorServiceTest {

    private FornecedorRepository repository;
    private AgendamentoRepository agendamentoRepository;
    private SimpleMeterRegistry metricas;
    private FornecedorService service;

    @BeforeEach
    void preparar() {
        repository = mock(FornecedorRepository.class);
        agendamentoRepository = mock(AgendamentoRepository.class);
        metricas = new SimpleMeterRegistry();
        service = new FornecedorService(repository, agendamentoRepository, new FornecedorMapper(),
                new Operacoes(metricas));
    }

    @Test
    @DisplayName("criar: grava e registra log INFO com o nome do fornecedor")
    void cria() {
        when(repository.save(any(Fornecedor.class))).thenReturn(Fabrica.fornecedor(4, "Frigorífico Sul"));

        try (CapturaDeLog logs = CapturaDeLog.de(FornecedorService.class)) {
            assertThat(service.criar(new FornecedorRequestDTO("Frigorífico Sul", "12.345.678/0001-90")).id())
                    .isEqualTo(4L);
            assertThat(logs.tem(Level.INFO, "Fornecedor 4 criado: Frigorífico Sul")).isTrue();
            assertThat(logs.temCampo("fornecedorId", 4)).isTrue();
        }
    }

    @Test
    @DisplayName("buscar inexistente: FornecedorNaoEncontradoException")
    void buscaInexistente() {
        when(repository.findById(8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(8L))
                .isInstanceOf(FornecedorNaoEncontradoException.class)
                .hasMessage("Fornecedor não encontrado: 8");
    }

    @Test
    @DisplayName("atualizar: troca nome e CNPJ")
    void atualiza() {
        Fornecedor fornecedor = Fabrica.fornecedor(4, "Frigorífico Sul");
        when(repository.findById(4L)).thenReturn(Optional.of(fornecedor));
        when(repository.save(fornecedor)).thenReturn(fornecedor);

        var alterado = service.atualizar(4L, new FornecedorRequestDTO("Frigorífico Sul Ltda", "98.765.432/0001-10"));

        assertThat(alterado.nome()).isEqualTo("Frigorífico Sul Ltda");
        assertThat(alterado.cnpj()).isEqualTo("98.765.432/0001-10");
    }

    @Test
    @DisplayName("R4: fornecedor com agendamentos não é excluído")
    void r4FornecedorComAgendamentos() {
        when(repository.findById(4L)).thenReturn(Optional.of(Fabrica.fornecedor(4, "Frigorífico Sul")));
        when(agendamentoRepository.existsByFornecedorId(4L)).thenReturn(true);

        assertThatThrownBy(() -> service.excluir(4L))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageStartingWith("R4");
        verify(repository, never()).delete(any(Fornecedor.class));
        assertThat(metricas.counter("agenda.regras.violadas", "regra", "R4").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("excluir sem agendamentos: apaga e conta a operação")
    void exclui() {
        Fornecedor fornecedor = Fabrica.fornecedor(4, "Frigorífico Sul");
        when(repository.findById(4L)).thenReturn(Optional.of(fornecedor));

        service.excluir(4L);

        verify(repository).delete(fornecedor);
        assertThat(metricas.counter("agenda.operacoes", "entidade", "fornecedor", "operacao", "excluir").count())
                .isEqualTo(1.0);
    }
}
