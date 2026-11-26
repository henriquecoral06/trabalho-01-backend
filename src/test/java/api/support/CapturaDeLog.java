// Pacote dos utilitários compartilhados pelos testes.
package api.support;

// Importa a lista de eventos capturados.
import java.util.List;

// Importa a API de log (SLF4J) para achar o logger de uma classe.
import org.slf4j.LoggerFactory;

// Importa os tipos do Logback, a implementação de log que o Spring Boot usa.
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

// Carta C2: prende um "gravador" (ListAppender) no logger de uma classe, para o
// teste conferir que a operação gerou o log certo, no nível certo.
//
//   try (CapturaDeLog logs = CapturaDeLog.de(AgendamentoService.class)) {
//       service.criar(dto);
//       assertThat(logs.tem(Level.INFO, "Agendamento 7 criado")).isTrue();
//   }
//
// AutoCloseable: o try-with-resources solta o gravador no fim, mesmo se o teste falhar.
public final class CapturaDeLog implements AutoCloseable {

    // O logger observado.
    private final Logger logger;

    // Guarda na memória cada evento de log.
    private final ListAppender<ILoggingEvent> gravador = new ListAppender<>();

    // Prende o gravador no logger da classe.
    private CapturaDeLog(Class<?> classe) {
        this.logger = (Logger) LoggerFactory.getLogger(classe);
        gravador.start();
        logger.addAppender(gravador);
    }

    // Começa a capturar os logs da classe.
    public static CapturaDeLog de(Class<?> classe) {
        return new CapturaDeLog(classe);
    }

    // Todos os eventos capturados até agora.
    public List<ILoggingEvent> eventos() {
        return gravador.list;
    }

    // Existe um evento deste nível cuja mensagem contém o trecho?
    public boolean tem(Level nivel, String trecho) {
        return gravador.list.stream()
                .anyMatch(e -> e.getLevel() == nivel && e.getFormattedMessage().contains(trecho));
    }

    // Existe um evento com este campo estruturado (addKeyValue) e este valor?
    public boolean temCampo(String chave, Object valor) {
        return gravador.list.stream()
                .filter(e -> e.getKeyValuePairs() != null)
                .flatMap(e -> e.getKeyValuePairs().stream())
                .anyMatch(kv -> kv.key.equals(chave) && String.valueOf(kv.value).equals(String.valueOf(valor)));
    }

    // Solta o gravador do logger.
    @Override
    public void close() {
        logger.detachAppender(gravador);
    }
}
