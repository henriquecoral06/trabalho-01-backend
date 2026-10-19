// Declara o pacote das classes de configuração da aplicação.
package api.config;

// Importa a anotação que permite declarar beans por método.
import org.springframework.context.annotation.Bean;
// Importa a anotação que marca a classe como fonte de configuração.
import org.springframework.context.annotation.Configuration;

// Importa o modelo Java do documento OpenAPI.
import io.swagger.v3.oas.models.OpenAPI;
// Importa o bloco "info" (título, versão, descrição) do documento.
import io.swagger.v3.oas.models.info.Info;

// Aula 09 -- O springdoc monta o documento OpenAPI sozinho, lendo os
// Controllers (rotas, verbos, status) e os DTOs (formato do JSON). Este bean
// só completa o cabeçalho do documento: o que aparece no topo do Swagger UI
// em http://localhost:8080/swagger-ui.html.
// Diz ao Spring que esta classe declara beans.
@Configuration
public class OpenApiConfig {

    // O objeto devolvido aqui vira a raiz do JSON em /v3/api-docs.
    @Bean
    public OpenAPI apiDaAgendaDeDocas() {
        // Monta o documento com o bloco "info" preenchido.
        return new OpenAPI()
                .info(new Info()
                        // Nome da API, em destaque no Swagger UI.
                        .title("Agenda de Docas")
                        // Versão do contrato -- aqui, a etapa em que ele foi publicado.
                        .version("Etapa 1")
                        // O que a API faz e em que pé ela está (aceita Markdown).
                        .description("API para o centro de distribuição agendar a descarga dos caminhões "
                                + "dos fornecedores nas docas. Tema: Logística e Cadeia de Suprimentos. "
                                + "Integrantes: Henrique Lucio Coral, Gabriel Dalmolin e Jhonatan Inácio. "
                                + "Nesta etapa respondem o cadastro de docas, o cadastro de fornecedores e o "
                                + "agendamento, com as regras R1, R2 e R3. Todo corpo de requisição e de "
                                + "resposta é um **DTO** e todo erro 400/404 sai no formato `ErroDTO`."));
    }
}
