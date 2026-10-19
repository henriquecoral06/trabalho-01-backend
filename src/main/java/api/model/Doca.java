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
// RESPONSABILIDADE: um portão do centro de distribuição onde o caminhão
//                   descarrega, gravado na tabela docas (migration V1).
// SITUAÇÃO: implementada.
// Diz ao JPA que esta classe será persistida no banco.
@Entity
// Liga a entidade à tabela docas criada pela migration V1.
@Table(name = "docas")
public class Doca {

    // Marca o atributo que representa a chave primária.
    @Id
    // Pede ao banco para gerar o id no momento do INSERT.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Guarda o identificador único da doca.
    private Long id;

    // Exige a identificação e limita o tamanho ao da coluna.
    @Column(nullable = false, length = 20)
    // Guarda como a doca é chamada no pátio, por exemplo D-01.
    private String identificacao;

    // Exige que a informação de câmara fria sempre tenha valor.
    @Column(nullable = false)
    // Diz se a doca recebe carga refrigerada. Usado pela regra R2.
    private boolean refrigerada;

    // Construtor sem argumentos exigido pelo Hibernate para reconstruir entidades.
    protected Doca() {
    }

    // Construtor usado para cadastrar uma doca nova (id = null, o banco gera no INSERT).
    public Doca(String identificacao, boolean refrigerada) {
        // Copia a identificação recebida para a entidade.
        this.identificacao = identificacao;
        // Copia a informação de câmara fria recebida para a entidade.
        this.refrigerada = refrigerada;
    }

    // Devolve o id da doca.
    public Long getId() {
        // Retorna o valor guardado no atributo id.
        return id;
    }

    // Devolve a identificação da doca.
    public String getIdentificacao() {
        // Retorna o valor guardado no atributo identificacao.
        return identificacao;
    }

    // Diz se a doca tem câmara fria.
    public boolean isRefrigerada() {
        // Retorna o valor guardado no atributo refrigerada.
        return refrigerada;
    }
}
