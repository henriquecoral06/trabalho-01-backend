// Define o pacote dos repositórios.
package api.repository;

// Importa o tipo de data e hora usado na consulta da regra R1.
import java.time.LocalDateTime;
// Importa o Optional devolvido pela consulta da regra R1.
import java.util.Optional;

// Importa o repositório do Spring Data JPA que já traz o CRUD pronto.
import org.springframework.data.jpa.repository.JpaRepository;

// Importa a entidade que este repositório acessa.
import api.model.Agendamento;

// CAMADA: Repository
// RESPONSABILIDADE: ler e gravar agendamentos na tabela agendamentos.
// SITUAÇÃO: implementada.
// <Agendamento, Long>: a entidade e o tipo do id. O Spring escreve a implementação.
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    // Regra R1: busca um agendamento da doca que se sobrepõe ao horário pedido.
    // Consulta pelo nome do método (Aula 08): DocaId = doca.id, InicioBefore = o
    // existente começa antes do fim pedido, FimAfter = e termina depois do início
    // pedido. Só consulta: quem decide se barra é o AgendamentoService.
    Optional<Agendamento> findFirstByDocaIdAndInicioBeforeAndFimAfter(Long docaId, LocalDateTime fim,
                                                                      LocalDateTime inicio);
}
