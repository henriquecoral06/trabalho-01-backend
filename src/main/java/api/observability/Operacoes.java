// Define o pacote das classes de observabilidade (carta C2).
package api.observability;

// Importa a API de log (SLF4J).
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// Importa a anotação que registra a classe como bean.
import org.springframework.stereotype.Component;

// Importa o registro de métricas do Micrometer.
import io.micrometer.core.instrument.MeterRegistry;

// CAMADA: Observabilidade
// RESPONSABILIDADE: contar as operações críticas e as regras de negócio que
//                   barraram pedidos, nas métricas do Actuator:
//
//   agenda_operacoes_total{entidade="agendamento",operacao="criar"}
//   agenda_regras_violadas_total{regra="R2"}
//
// Com isso o coordenador vê, no /actuator/prometheus (ou num painel do Grafana),
// quantos agendamentos entram por hora e QUAL regra mais barra fornecedores --
// se a R3 domina, os fornecedores estão avisando em cima da hora.
//
// Os logs de cada operação ficam nos Services (com o Logger de cada um, para o
// nome da classe aparecer no log); aqui fica só o que é comum a todos.
// SITUAÇÃO: implementada.
@Component
public class Operacoes {

    // Logger desta classe: registra a regra violada em WARN.
    private static final Logger log = LoggerFactory.getLogger(Operacoes.class);

    // Onde as métricas são registradas (o Spring injeta o do Prometheus).
    private final MeterRegistry metricas;

    // Recebe o registro de métricas injetado pelo Spring (nos testes, um SimpleMeterRegistry).
    public Operacoes(MeterRegistry metricas) {
        this.metricas = metricas;
    }

    // Soma 1 na operação: contar("doca", "criar").
    public void contar(String entidade, String operacao) {
        metricas.counter("agenda.operacoes", "entidade", entidade, "operacao", operacao).increment();
    }

    // Uma regra de negócio barrou o pedido: registra em WARN (não é erro da API,
    // é o sistema funcionando), soma na métrica da regra e devolve a mensagem
    // para o Service lançar a RegraDeNegocioException.
    public String regraViolada(String regra, String entidade, String mensagem) {
        log.atWarn()
                .addKeyValue("regra", regra)
                .addKeyValue("entidade", entidade)
                .log("Pedido recusado ({}): {}", regra, mensagem);
        metricas.counter("agenda.regras.violadas", "regra", regra).increment();
        return mensagem;
    }
}
