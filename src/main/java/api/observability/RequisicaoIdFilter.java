// Define o pacote das classes de observabilidade (carta C2).
package api.observability;

// Importa a exceção de entrada e saída exigida pela assinatura do filtro.
import java.io.IOException;
// Importa o gerador de identificadores aleatórios.
import java.util.UUID;

// Importa a API de log (SLF4J) e o MDC, o "bolso" de contexto de cada linha de log.
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
// Importa a ordem do filtro: este precisa rodar antes de todos.
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
// Importa a anotação que registra o filtro como bean.
import org.springframework.stereotype.Component;
// Importa o atributo que guarda o padrão da rota (/docas/{id}, e não /docas/7).
import org.springframework.web.servlet.HandlerMapping;
// Importa a classe-base de filtro que roda uma vez por requisição.
import org.springframework.web.filter.OncePerRequestFilter;

// Importa os tipos da API de Servlet.
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// CAMADA: Observabilidade
// RESPONSABILIDADE: dar um id a cada requisição e registrar uma linha de log de
//                   acesso no fim dela (método, rota, status e tempo).
//
// O id vai para o MDC: a partir daí TODA linha de log escrita durante essa
// requisição -- no Controller, no Service, no ApiExceptionHandler -- sai com o
// mesmo id (veja logging.pattern.level no application.properties). Ele também
// volta para o cliente no cabeçalho X-Request-Id: quando o fornecedor reclama
// "meu agendamento foi recusado", o coordenador procura esse id no log e acha
// a requisição inteira, inclusive qual regra recusou.
//
// O nível da linha de acesso segue o status: INFO para 2xx/3xx, WARN para 4xx
// (o cliente errou ou uma regra barrou) e ERROR para 5xx (a API falhou).
// SITUAÇÃO: implementada (mesmo padrão do exemplo da Aula 13).
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequisicaoIdFilter extends OncePerRequestFilter {

    // Nome do cabeçalho HTTP com o id da requisição.
    public static final String CABECALHO = "X-Request-Id";

    // Chave do id no MDC (é o nome que aparece no log JSON).
    public static final String CHAVE_MDC = "requisicaoId";

    // Logger desta classe.
    private static final Logger log = LoggerFactory.getLogger(RequisicaoIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // Reaproveita o id que o cliente mandou (ex.: um gateway na frente da API) ou cria um curto.
        String id = request.getHeader(CABECALHO);
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString().substring(0, 8);
        }
        // A partir daqui, toda linha de log desta requisição carrega o id.
        MDC.put(CHAVE_MDC, id);
        // Devolve o id para o cliente.
        response.setHeader(CABECALHO, id);
        // Marca o começo, para medir o tempo da requisição.
        long inicio = System.nanoTime();
        try {
            // Segue para o Controller.
            chain.doFilter(request, response);
        } finally {
            // Mesmo se der erro, registra o acesso e limpa o MDC (a thread volta para o pool).
            registrarAcesso(request, response, inicio);
            MDC.remove(CHAVE_MDC);
        }
    }

    // Uma linha por requisição: "POST /agendamentos -> 400 (12 ms)".
    private void registrarAcesso(HttpServletRequest request, HttpServletResponse response, long inicio) {
        String uri = request.getRequestURI();
        // Health check e métricas são chamados a toda hora pelo Prometheus: não poluem o log.
        if (uri.startsWith("/actuator")) {
            return;
        }
        // O padrão da rota agrupa melhor no log: /docas/{id} em vez de /docas/1, /docas/2...
        Object padrao = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String rota = padrao != null ? padrao.toString() : uri;
        long ms = (System.nanoTime() - inicio) / 1_000_000;
        int status = response.getStatus();
        // Nível conforme o status; os campos separados vão para o log JSON.
        var evento = status >= 500 ? log.atError() : status >= 400 ? log.atWarn() : log.atInfo();
        evento.addKeyValue("metodo", request.getMethod())
                .addKeyValue("rota", rota)
                .addKeyValue("status", status)
                .addKeyValue("duracaoMs", ms)
                .log("{} {} -> {} ({} ms)", request.getMethod(), rota, status, ms);
    }
}
