// Mesmo pacote do AgendamentoController.
package api.controller;

// Importa os recursos do Mockito e do MockMvc.
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Importa o tipo de data e hora do agendamento.
import java.time.LocalDateTime;

// Importa o JUnit 5.
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
// Importa o suporte do Spring Boot a testes da camada web.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Importa o nível de log usado nas conferências.
import ch.qos.logback.classic.Level;

// Importa as classes do projeto usadas no teste.
import api.dto.AgendamentoRequestDTO;
import api.dto.AgendamentoResponseDTO;
import api.dto.DocaResponseDTO;
import api.dto.FornecedorResponseDTO;
import api.observability.RequisicaoIdFilter;
import api.service.AgendamentoService;
import api.service.DocaNaoEncontradaException;
import api.service.RegraDeNegocioException;
import api.support.CapturaDeLog;

// Carta C2: testes da camada web do /agendamentos com @WebMvcTest.
//
// Sobe só o Spring MVC (Controller, ApiExceptionHandler e o RequisicaoIdFilter),
// sem banco: o AgendamentoService é um mock. Confere o que o cliente da API vê
// -- status, formato do ErroDTO, cabeçalho X-Request-Id -- e o log de erro.
@WebMvcTest(AgendamentoController.class)
class AgendamentoControllerTest {

    // Faz requisições HTTP de mentira, sem abrir porta.
    @Autowired
    private MockMvc mvc;

    // Troca o Service de verdade por um mock dentro do contexto do Spring.
    @MockitoBean
    private AgendamentoService service;

    // Um corpo válido de POST /agendamentos.
    private static final String CORPO_VALIDO = """
            {"fornecedorId": 1, "docaId": 1, "inicio": "2026-12-10T08:00:00",
             "fim": "2026-12-10T10:00:00", "cargaRefrigerada": true}
            """;

    @Test
    @DisplayName("POST válido: 201 com o agendamento e o cabeçalho X-Request-Id")
    void criaComSucesso() throws Exception {
        when(service.criar(any(AgendamentoRequestDTO.class))).thenReturn(new AgendamentoResponseDTO(7L,
                new FornecedorResponseDTO(1L, "Laticínios Serra Azul", "12.345.678/0001-90"),
                new DocaResponseDTO(1L, "D-01", true),
                LocalDateTime.of(2026, 12, 10, 8, 0), LocalDateTime.of(2026, 12, 10, 10, 0), true));

        mvc.perform(post("/agendamentos").contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.doca.identificacao").value("D-01"))
                .andExpect(header().exists(RequisicaoIdFilter.CABECALHO));
    }

    @Test
    @DisplayName("o X-Request-Id enviado pelo cliente volta igual na resposta")
    void reaproveitaRequisicaoId() throws Exception {
        mvc.perform(delete("/agendamentos/7").header(RequisicaoIdFilter.CABECALHO, "meu-id-123"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(RequisicaoIdFilter.CABECALHO, "meu-id-123"));
        verify(service).excluir(7L);
    }

    @Test
    @DisplayName("corpo sem os campos obrigatórios: 400 com a lista de campos, e o Service nem é chamado")
    void validacao() throws Exception {
        try (CapturaDeLog logs = CapturaDeLog.de(ApiExceptionHandler.class)) {
            mvc.perform(post("/agendamentos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.campos.length()").value(5))
                    .andExpect(jsonPath("$.campos[0].campo").value("cargaRefrigerada"));
            assertThat(logs.tem(Level.WARN, "Validação reprovou POST /agendamentos")).isTrue();
        }
        verify(service, never()).criar(any());
    }

    @Test
    @DisplayName("regra de negócio violada: 400 com a mensagem da regra no ErroDTO")
    void regraVioladaVira400() throws Exception {
        when(service.criar(any())).thenThrow(new RegraDeNegocioException("R2: a carga é refrigerada e a doca D-02 "
                + "não é refrigerada"));

        mvc.perform(post("/agendamentos").contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("R2: a carga é refrigerada e a doca D-02 não é refrigerada"))
                .andExpect(jsonPath("$.caminho").value("/agendamentos"));
    }

    @Test
    @DisplayName("doca inexistente: 404 no formato ErroDTO e log WARN")
    void naoEncontradoVira404() throws Exception {
        when(service.criar(any())).thenThrow(new DocaNaoEncontradaException(99L));

        try (CapturaDeLog logs = CapturaDeLog.de(ApiExceptionHandler.class)) {
            mvc.perform(post("/agendamentos").contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.erro").value("Not Found"))
                    .andExpect(jsonPath("$.mensagem").value("Doca não encontrada: 99"));
            assertThat(logs.tem(Level.WARN, "Doca não encontrada: 99")).isTrue();
        }
    }

    @Test
    @DisplayName("erro inesperado: 500 sem detalhe interno para o cliente, e log ERROR com a exceção")
    void erroInesperadoVira500() throws Exception {
        when(service.buscarPorId(1L)).thenThrow(new IllegalStateException("conexão com o banco caiu"));

        try (CapturaDeLog logs = CapturaDeLog.de(ApiExceptionHandler.class)) {
            mvc.perform(get("/agendamentos/1"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.not(
                            org.hamcrest.Matchers.containsString("banco"))));
            assertThat(logs.tem(Level.ERROR, "Erro inesperado em GET /agendamentos/1")).isTrue();
            // A stack trace foi junto para o log.
            assertThat(logs.eventos().getLast().getThrowableProxy().getMessage()).isEqualTo("conexão com o banco caiu");
        }
    }

    @Test
    @DisplayName("verbo não suportado continua 405 (o tratador geral não transforma tudo em 500)")
    void verboNaoSuportado() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/agendamentos/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    @DisplayName("GET com filtros: repassa os parâmetros para o Service")
    void listaComFiltros() throws Exception {
        when(service.listar(eq(1L), eq(null), eq(true), any(), any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        mvc.perform(get("/agendamentos").param("docaId", "1").param("cargaRefrigerada", "true")
                        .param("de", "2026-12-01").param("ate", "2026-12-05"))
                .andExpect(status().isOk());
        verify(service).listar(eq(1L), eq(null), eq(true), eq(java.time.LocalDate.of(2026, 12, 1)),
                eq(java.time.LocalDate.of(2026, 12, 5)), any());
    }

    @Test
    @DisplayName("data em formato errado no filtro: 400 dizendo qual parâmetro")
    void dataInvalida() throws Exception {
        mvc.perform(get("/agendamentos").param("de", "01/12/2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.startsWith("Parâmetro inválido: de")));
    }
}
