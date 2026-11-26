// Define o pacote dos repositórios.
package api.repository;

// Importa o repositório do Spring Data JPA que já traz o CRUD pronto.
import org.springframework.data.jpa.repository.JpaRepository;
// Importa o suporte a consultas montadas por partes (os filtros opcionais da listagem).
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

// Importa a entidade que este repositório acessa.
import api.model.Doca;

// CAMADA: Repository
// RESPONSABILIDADE: ler e gravar docas na tabela docas.
// SITUAÇÃO: implementada. O save, o findById, o deleteById e o findAll(Pageable)
//           vêm do JpaRepository; o findAll(Specification, Pageable), que a
//           listagem com filtros usa, vem do JpaSpecificationExecutor.
// <Doca, Long>: a entidade e o tipo do id. O Spring escreve a implementação.
public interface DocaRepository extends JpaRepository<Doca, Long>, JpaSpecificationExecutor<Doca> {
}
