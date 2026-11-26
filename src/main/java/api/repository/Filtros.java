// Define o pacote dos repositórios.
package api.repository;

// Importa o tipo de data e hora usado no filtro por período.
import java.time.LocalDateTime;

// Importa a interface que representa um pedaço de WHERE montado em Java.
import org.springframework.data.jpa.domain.Specification;

// Importa as entidades filtradas.
import api.model.Agendamento;
import api.model.Doca;
import api.model.Fornecedor;

// CAMADA: Repository
// RESPONSABILIDADE: os filtros opcionais das listagens (GET /docas,
//                   GET /fornecedores e GET /agendamentos). Cada método devolve
//                   um pedaço do WHERE; quando o parâmetro não veio (null), o
//                   pedaço devolve null e o Spring Data simplesmente o ignora.
//                   O Service junta os pedaços com Specification.allOf(...).
// SITUAÇÃO: implementada.
public final class Filtros {

    // Classe só de métodos estáticos: ninguém precisa criar um objeto dela.
    private Filtros() {
    }

    // ---------------------------------------------------------------- Doca

    // ?identificacao=D-0 : identificação contém o texto, sem diferenciar maiúscula.
    public static Specification<Doca> docaIdentificacaoContem(String texto) {
        // Sem texto, sem filtro.
        return (root, query, cb) -> vazio(texto) ? null
                : cb.like(cb.lower(root.get("identificacao")), "%" + texto.trim().toLowerCase() + "%");
    }

    // ?refrigerada=true : só as docas com (ou sem) câmara fria.
    public static Specification<Doca> docaRefrigerada(Boolean refrigerada) {
        // Sem valor, sem filtro.
        return (root, query, cb) -> refrigerada == null ? null : cb.equal(root.get("refrigerada"), refrigerada);
    }

    // ---------------------------------------------------------------- Fornecedor

    // ?nome=serra : nome contém o texto, sem diferenciar maiúscula.
    public static Specification<Fornecedor> fornecedorNomeContem(String texto) {
        // Sem texto, sem filtro.
        return (root, query, cb) -> vazio(texto) ? null
                : cb.like(cb.lower(root.get("nome")), "%" + texto.trim().toLowerCase() + "%");
    }

    // ?cnpj=12.345 : CNPJ começa com o texto.
    public static Specification<Fornecedor> fornecedorCnpjComeca(String texto) {
        // Sem texto, sem filtro.
        return (root, query, cb) -> vazio(texto) ? null : cb.like(root.get("cnpj"), texto.trim() + "%");
    }

    // ---------------------------------------------------------------- Agendamento

    // ?docaId=1 : só os agendamentos da doca.
    public static Specification<Agendamento> agendamentoDaDoca(Long docaId) {
        // Sem id, sem filtro.
        return (root, query, cb) -> docaId == null ? null : cb.equal(root.get("doca").get("id"), docaId);
    }

    // ?fornecedorId=1 : só os agendamentos do fornecedor.
    public static Specification<Agendamento> agendamentoDoFornecedor(Long fornecedorId) {
        // Sem id, sem filtro.
        return (root, query, cb) -> fornecedorId == null ? null
                : cb.equal(root.get("fornecedor").get("id"), fornecedorId);
    }

    // ?cargaRefrigerada=true : só as cargas refrigeradas (ou só as secas).
    public static Specification<Agendamento> agendamentoCargaRefrigerada(Boolean refrigerada) {
        // Sem valor, sem filtro.
        return (root, query, cb) -> refrigerada == null ? null
                : cb.equal(root.get("cargaRefrigerada"), refrigerada);
    }

    // ?de=2026-12-01 : agendamentos que começam a partir deste momento.
    public static Specification<Agendamento> agendamentoComecaAPartirDe(LocalDateTime de) {
        // Sem data, sem filtro.
        return (root, query, cb) -> de == null ? null : cb.greaterThanOrEqualTo(root.get("inicio"), de);
    }

    // ?ate=2026-12-05 : agendamentos que começam antes deste momento.
    public static Specification<Agendamento> agendamentoComecaAntesDe(LocalDateTime ate) {
        // Sem data, sem filtro.
        return (root, query, cb) -> ate == null ? null : cb.lessThan(root.get("inicio"), ate);
    }

    // Texto nulo ou só com espaços conta como "não filtrar".
    private static boolean vazio(String texto) {
        // isBlank() cobre "" e "   ".
        return texto == null || texto.isBlank();
    }
}
