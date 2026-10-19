// Define o pacote dos repositórios.
package api.repository;

// Importa o repositório do Spring Data JPA que já traz o CRUD pronto.
import org.springframework.data.jpa.repository.JpaRepository;

// Importa a entidade que este repositório acessa.
import api.model.Fornecedor;

// CAMADA: Repository
// RESPONSABILIDADE: ler e gravar fornecedores na tabela fornecedores.
// SITUAÇÃO: implementada.
// <Fornecedor, Long>: a entidade e o tipo do id. O Spring escreve a implementação.
public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {
}
