// Define o pacote onde as entidades ficam organizadas.
package api.model;

// Importa o tipo de data e hora usado no início e no fim do agendamento.
import java.time.LocalDateTime;

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
// Importa a anotação que diz qual coluna guarda a chave estrangeira.
import jakarta.persistence.JoinColumn;
// Importa a anotação do relacionamento "muitos para um".
import jakarta.persistence.ManyToOne;
// Importa a anotação que define o nome da tabela.
import jakarta.persistence.Table;

// CAMADA: Model
// RESPONSABILIDADE: a reserva de uma doca, num horário, para a descarga de um
//                   fornecedor, gravada na tabela agendamentos (migration V3).
//                   É aqui que estão os dois relacionamentos do modelo: muitos
//                   agendamentos para um fornecedor, e muitos para uma doca.
// SITUAÇÃO: implementada.
// Diz ao JPA que esta classe será persistida no banco.
@Entity
// Liga a entidade à tabela agendamentos criada pela migration V3.
@Table(name = "agendamentos")
public class Agendamento {

    // Marca o atributo que representa a chave primária.
    @Id
    // Pede ao banco para gerar o id no momento do INSERT.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Guarda o identificador único do agendamento.
    private Long id;

    // Muitos agendamentos podem ser do mesmo fornecedor.
    @ManyToOne
    // A chave estrangeira fica na coluna fornecedor_id, obrigatória.
    @JoinColumn(name = "fornecedor_id", nullable = false)
    // Guarda quem vai entregar.
    private Fornecedor fornecedor;

    // Muitos agendamentos podem ser da mesma doca, em horários diferentes.
    @ManyToOne
    // A chave estrangeira fica na coluna doca_id, obrigatória.
    @JoinColumn(name = "doca_id", nullable = false)
    // Guarda em qual doca o caminhão vai descarregar.
    private Doca doca;

    // Liga o atributo à coluna inicio, obrigatória.
    @Column(nullable = false)
    // Dia e hora em que a doca começa a ficar reservada.
    private LocalDateTime inicio;

    // Liga o atributo à coluna fim, obrigatória.
    @Column(nullable = false)
    // Dia e hora em que a doca fica livre de novo.
    private LocalDateTime fim;

    // Liga o atributo à coluna carga_refrigerada, obrigatória.
    @Column(name = "carga_refrigerada", nullable = false)
    // Diz se a carga precisa de câmara fria. Usado pela regra R2.
    private boolean cargaRefrigerada;

    // Construtor sem argumentos exigido pelo Hibernate para reconstruir entidades.
    protected Agendamento() {
    }

    // Construtor usado para registrar um agendamento novo (id = null, o banco gera no INSERT).
    public Agendamento(Fornecedor fornecedor, Doca doca, LocalDateTime inicio, LocalDateTime fim,
                       boolean cargaRefrigerada) {
        // Copia o fornecedor recebido para a entidade.
        this.fornecedor = fornecedor;
        // Copia a doca recebida para a entidade.
        this.doca = doca;
        // Copia o início recebido para a entidade.
        this.inicio = inicio;
        // Copia o fim recebido para a entidade.
        this.fim = fim;
        // Copia a informação de carga refrigerada para a entidade.
        this.cargaRefrigerada = cargaRefrigerada;
    }

    // Devolve o id do agendamento.
    public Long getId() {
        // Retorna o valor guardado no atributo id.
        return id;
    }

    // Devolve o fornecedor do agendamento.
    public Fornecedor getFornecedor() {
        // Retorna o valor guardado no atributo fornecedor.
        return fornecedor;
    }

    // Devolve a doca reservada.
    public Doca getDoca() {
        // Retorna o valor guardado no atributo doca.
        return doca;
    }

    // Devolve o início da reserva.
    public LocalDateTime getInicio() {
        // Retorna o valor guardado no atributo inicio.
        return inicio;
    }

    // Devolve o fim da reserva.
    public LocalDateTime getFim() {
        // Retorna o valor guardado no atributo fim.
        return fim;
    }

    // Diz se a carga precisa de câmara fria.
    public boolean isCargaRefrigerada() {
        // Retorna o valor guardado no atributo cargaRefrigerada.
        return cargaRefrigerada;
    }
}
