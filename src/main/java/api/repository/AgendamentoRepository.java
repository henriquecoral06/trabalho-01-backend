// Define o pacote dos repositórios.
package api.repository;

// Importa o repositório do Spring Data JPA que já traz o CRUD pronto.
import org.springframework.data.jpa.repository.JpaRepository;

// Importa a entidade que este repositório acessa.
import api.model.Agendamento;

// CAMADA: Repository
// RESPONSABILIDADE: ler e gravar agendamentos na tabela agendamentos.
// SITUAÇÃO: montada. A consulta da regra R1 entra quando o POST /agendamentos
//           for implementado.
// <Agendamento, Long>: a entidade e o tipo do id. O Spring escreve a implementação.
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {
}
