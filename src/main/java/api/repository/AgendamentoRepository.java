// Define o pacote dos repositórios.
package api.repository;

// Importa o tipo de data e hora usado nas consultas.
import java.time.LocalDateTime;
// Importa a lista devolvida na consulta da ocupação.
import java.util.List;
// Importa o Optional devolvido pela consulta da regra R1.
import java.util.Optional;

// Importa o repositório do Spring Data JPA que já traz o CRUD pronto.
import org.springframework.data.jpa.repository.JpaRepository;
// Importa o suporte a consultas montadas por partes (os filtros opcionais da listagem).
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

// Importa a entidade que este repositório acessa.
import api.model.Agendamento;

// CAMADA: Repository
// RESPONSABILIDADE: ler e gravar agendamentos na tabela agendamentos.
// SITUAÇÃO: implementada.
// <Agendamento, Long>: a entidade e o tipo do id. O Spring escreve a implementação.
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long>,
        JpaSpecificationExecutor<Agendamento> {

    // Regra R1: busca um agendamento da doca que se sobrepõe ao horário pedido.
    // Consulta pelo nome do método (Aula 08): DocaId = doca.id, InicioBefore = o
    // existente começa antes do fim pedido, FimAfter = e termina depois do início
    // pedido. Só consulta: quem decide se barra é o AgendamentoService.
    Optional<Agendamento> findFirstByDocaIdAndInicioBeforeAndFimAfter(Long docaId, LocalDateTime fim,
                                                                      LocalDateTime inicio);

    // Regra R1 na remarcação (PUT): a mesma consulta, mas ignorando o próprio
    // agendamento -- senão ele "se sobreporia" a si mesmo e nunca daria para remarcar.
    Optional<Agendamento> findFirstByDocaIdAndIdNotAndInicioBeforeAndFimAfter(Long docaId, Long id,
                                                                              LocalDateTime fim,
                                                                              LocalDateTime inicio);

    // Regra R4: a doca tem algum agendamento? Se tiver, não pode ser excluída.
    boolean existsByDocaId(Long docaId);

    // Regra R4: o fornecedor tem algum agendamento? Se tiver, não pode ser excluído.
    boolean existsByFornecedorId(Long fornecedorId);

    // Ocupação das docas: todo agendamento que encosta no período pedido
    // (começa antes do fim do período e termina depois do início dele).
    List<Agendamento> findByInicioBeforeAndFimAfter(LocalDateTime fimDoPeriodo, LocalDateTime inicioDoPeriodo);
}
