// Define o pacote onde as entidades ficam organizadas.
package api.model;

// Importa a anotação que configura colunas da tabela.
import jakarta.persistence.Column;
// Importa a anotação que transforma a classe em entidade JPA.
import jakarta.persistence.Entity;
// Importa a anotação que ativa a geração automática do id.
import jakarta.persistence.GeneratedValue;
// Importa as estratégias disponíveis para gerar ids.
import jakarta.persistence.GenerationType;
// Importa a anotação que marca a chave primária.
import jakarta.persistence.Id;
// Importa a anotação que define o nome da tabela.
import jakarta.persistence.Table;

// CAMADA: Model
// RESPONSABILIDADE: uma empresa que entrega mercadoria no centro de distribuição,
//                   gravada na tabela fornecedores (migration V2).
// SITUAÇÃO: implementada.
// Diz ao JPA que esta classe será persistida no banco.
@Entity
// Liga a entidade à tabela fornecedores criada pela migration V2.
@Table(name = "fornecedores")
public class Fornecedor {

    // Marca o atributo que representa a chave primária.
    @Id
    // Pede ao banco para gerar o id no momento do INSERT.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Guarda o identificador único do fornecedor.
    private Long id;

    // Exige que o nome nunca seja nulo no banco.
    @Column(nullable = false)
    // Guarda o nome do fornecedor.
    private String nome;

    // Exige o CNPJ e limita o tamanho ao da coluna.
    @Column(nullable = false, length = 18)
    // Guarda o CNPJ do fornecedor.
    private String cnpj;

    // Construtor sem argumentos exigido pelo Hibernate para reconstruir entidades.
    protected Fornecedor() {
    }

    // Construtor usado para cadastrar um fornecedor novo (id = null, o banco gera no INSERT).
    public Fornecedor(String nome, String cnpj) {
        // Copia o nome recebido para a entidade.
        this.nome = nome;
        // Copia o CNPJ recebido para a entidade.
        this.cnpj = cnpj;
    }

    // Devolve o id do fornecedor.
    public Long getId() {
        // Retorna o valor guardado no atributo id.
        return id;
    }

    // Devolve o nome do fornecedor.
    public String getNome() {
        // Retorna o valor guardado no atributo nome.
        return nome;
    }

    // Devolve o CNPJ do fornecedor.
    public String getCnpj() {
        // Retorna o valor guardado no atributo cnpj.
        return cnpj;
    }
}
