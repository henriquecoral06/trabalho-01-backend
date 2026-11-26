// Pacote dos utilitários compartilhados pelos testes.
package api.support;

// Importa o tipo de data e hora do agendamento.
import java.time.LocalDateTime;

// Importa o utilitário do Spring que preenche campos privados (o id é gerado pelo banco).
import org.springframework.test.util.ReflectionTestUtils;

// Importa as entidades montadas nos testes.
import api.model.Agendamento;
import api.model.Doca;
import api.model.Fornecedor;

// Monta entidades já com id, como se tivessem vindo do banco. As entidades não
// têm setId (quem gera o id é o PostgreSQL), então o id entra por reflexão.
public final class Fabrica {

    // Classe só de métodos estáticos.
    private Fabrica() {
    }

    // Uma doca com id.
    public static Doca doca(long id, String identificacao, boolean refrigerada) {
        Doca doca = new Doca(identificacao, refrigerada);
        ReflectionTestUtils.setField(doca, "id", id);
        return doca;
    }

    // Um fornecedor com id.
    public static Fornecedor fornecedor(long id, String nome) {
        Fornecedor fornecedor = new Fornecedor(nome, "12.345.678/0001-90");
        ReflectionTestUtils.setField(fornecedor, "id", id);
        return fornecedor;
    }

    // Um agendamento com id.
    public static Agendamento agendamento(long id, Fornecedor fornecedor, Doca doca, LocalDateTime inicio,
                                          LocalDateTime fim, boolean cargaRefrigerada) {
        Agendamento agendamento = new Agendamento(fornecedor, doca, inicio, fim, cargaRefrigerada);
        ReflectionTestUtils.setField(agendamento, "id", id);
        return agendamento;
    }
}
