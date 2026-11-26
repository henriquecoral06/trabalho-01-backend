// Define o pacote da camada de regras de negócio.
package api.service;

// Importa os tipos de data e de duração usados no cálculo.
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
// Importa as coleções usadas para agrupar os agendamentos por doca.
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Importa a ordenação usada para listar as docas.
import org.springframework.data.domain.Sort;
// Importa a anotação que registra a classe como serviço Spring.
import org.springframework.stereotype.Service;
// Importa a anotação de transação somente leitura.
import org.springframework.transaction.annotation.Transactional;

// Importa os DTOs de saída da ocupação.
import api.dto.OcupacaoDTO;
import api.dto.OcupacaoDocaDTO;
// Importa as entidades usadas no cálculo.
import api.model.Agendamento;
import api.model.Doca;
// Importa os repositórios consultados.
import api.repository.AgendamentoRepository;
import api.repository.DocaRepository;

// CAMADA: Service
// RESPONSABILIDADE: aplicação prática "Visualização de Dados". Calcula, a partir
//                   dos agendamentos que já existem, quanto cada doca fica
//                   ocupada num período -- os números que um dashboard do
//                   coordenador do CD mostraria. Não cria entidade nova: usa
//                   Doca e Agendamento.
// REGRAS: o período tem no máximo 31 dias e "ate" não pode vir antes de "de".
// ROTAS ATENDIDAS: GET /docas/ocupacao.
// SITUAÇÃO: implementada.
@Service
public class OcupacaoService {

    // Maior período aceito, para a consulta não varrer a agenda inteira.
    public static final int MAXIMO_DE_DIAS = 31;

    // A partir de quanto % de ocupação a doca entra nos alertas.
    public static final double LIMITE_DE_ALERTA = 85.0;

    // Acesso à tabela docas: entram também as docas sem nenhum agendamento.
    private final DocaRepository docaRepository;

    // Acesso à tabela agendamentos.
    private final AgendamentoRepository agendamentoRepository;

    // Recebe as dependências que o Spring injeta automaticamente.
    public OcupacaoService(DocaRepository docaRepository, AgendamentoRepository agendamentoRepository) {
        // Guarda as dependências recebidas nos atributos da classe.
        this.docaRepository = docaRepository;
        this.agendamentoRepository = agendamentoRepository;
    }

    // GET /docas/ocupacao?de=&ate= : a ocupação de todas as docas no período.
    @Transactional(readOnly = true)
    public OcupacaoDTO calcular(LocalDate de, LocalDate ate) {
        // O período precisa fazer sentido antes de qualquer consulta.
        if (ate.isBefore(de)) {
            throw new RegraDeNegocioException("O fim do período (" + ate + ") é antes do início (" + de + ")");
        }
        // +1: "ate" entra inteiro (de 01/12 a 01/12 é um dia).
        long dias = ChronoUnit.DAYS.between(de, ate) + 1;
        if (dias > MAXIMO_DE_DIAS) {
            throw new RegraDeNegocioException("O período pode ter no máximo " + MAXIMO_DE_DIAS + " dias");
        }
        // O período vai de 00:00 do primeiro dia até 00:00 do dia seguinte ao último.
        LocalDateTime inicio = de.atStartOfDay();
        LocalDateTime fim = ate.plusDays(1).atStartOfDay();
        double horasDisponiveis = dias * 24.0;

        // Uma consulta só: todo agendamento que encosta no período, agrupado por doca.
        Map<Long, List<Agendamento>> porDoca = agendamentoRepository.findByInicioBeforeAndFimAfter(fim, inicio)
                .stream()
                .collect(Collectors.groupingBy(a -> a.getDoca().getId()));

        // Uma linha por doca cadastrada -- a doca parada também aparece, com 0%.
        List<OcupacaoDocaDTO> linhas = docaRepository.findAll(Sort.by("identificacao")).stream()
                .map(doca -> linha(doca, porDoca.getOrDefault(doca.getId(), List.of()), inicio, fim,
                        horasDisponiveis))
                // Da mais ocupada para a menos ocupada: o que o dashboard mostra primeiro.
                .sorted(Comparator.comparingDouble(OcupacaoDocaDTO::taxaOcupacao).reversed())
                .toList();

        // Taxa geral: soma das horas agendadas / soma das horas disponíveis.
        double horasAgendadasTotal = linhas.stream().mapToDouble(OcupacaoDocaDTO::horasAgendadas).sum();
        double taxaGeral = linhas.isEmpty() ? 0 : percentual(horasAgendadasTotal, horasDisponiveis * linhas.size());

        return new OcupacaoDTO(de, ate, horasDisponiveis, taxaGeral, linhas, alertas(linhas));
    }

    // Monta a linha de uma doca: quantos agendamentos, quantas horas e qual a taxa.
    private OcupacaoDocaDTO linha(Doca doca, List<Agendamento> agendamentos, LocalDateTime inicio,
                                  LocalDateTime fim, double horasDisponiveis) {
        // Só conta a parte do agendamento que cai dentro do período.
        double horas = agendamentos.stream().mapToDouble(a -> horasDentro(a, inicio, fim)).sum();
        // Câmara fria ocupada por carga seca: só faz sentido em doca refrigerada.
        double horasSecaNaFria = doca.isRefrigerada()
                ? agendamentos.stream().filter(a -> !a.isCargaRefrigerada())
                        .mapToDouble(a -> horasDentro(a, inicio, fim)).sum()
                : 0;
        return new OcupacaoDocaDTO(doca.getId(), doca.getIdentificacao(), doca.isRefrigerada(),
                agendamentos.size(), arredondar(horas), percentual(horas, horasDisponiveis),
                arredondar(horasSecaNaFria));
    }

    // Os alertas que o coordenador precisa ver, em português.
    private List<String> alertas(List<OcupacaoDocaDTO> linhas) {
        List<String> alertas = new ArrayList<>();
        for (OcupacaoDocaDTO linha : linhas) {
            // Doca quase lotada: um atraso de caminhão já vira fila no pátio.
            if (linha.taxaOcupacao() >= LIMITE_DE_ALERTA) {
                alertas.add("A doca " + linha.identificacao() + " está " + linha.taxaOcupacao()
                        + "% ocupada: perto do limite");
            }
            // Câmara fria gasta com carga seca: dá para mover essa carga para uma doca comum.
            if (linha.horasDeCargaSecaEmDocaRefrigerada() > 0) {
                alertas.add("A doca refrigerada " + linha.identificacao() + " tem "
                        + linha.horasDeCargaSecaEmDocaRefrigerada()
                        + " h ocupadas por carga seca: essas cargas podem ir para uma doca comum");
            }
        }
        return alertas;
    }

    // Horas do agendamento que caem dentro do período [inicio, fim).
    private static double horasDentro(Agendamento a, LocalDateTime inicio, LocalDateTime fim) {
        // Corta o que passa da borda do período, dos dois lados.
        LocalDateTime de = a.getInicio().isBefore(inicio) ? inicio : a.getInicio();
        LocalDateTime ate = a.getFim().isAfter(fim) ? fim : a.getFim();
        // Em minutos, para não perder as meias horas.
        return Math.max(0, Duration.between(de, ate).toMinutes()) / 60.0;
    }

    // parte / todo, em %, com uma casa decimal.
    private static double percentual(double parte, double todo) {
        return todo == 0 ? 0 : arredondar(parte * 100.0 / todo);
    }

    // Uma casa decimal: 12.345 vira 12.3.
    private static double arredondar(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }
}
