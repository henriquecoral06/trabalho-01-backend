// Declara o pacote das classes de configuração da aplicação.
package api.config;

// Importa a anotação que marca a classe como fonte de configuração.
import org.springframework.context.annotation.Configuration;
// Importa a anotação que configura o suporte web do Spring Data (Pageable, Page).
import org.springframework.data.web.config.EnableSpringDataWebSupport;

// Liga o formato estável de JSON para as páginas (Page) devolvidas nas listagens:
//
//   { "content": [ ... ], "page": { "size": 10, "number": 0, "totalElements": 3, "totalPages": 1 } }
//
// Sem esta linha, o Spring serializa a classe PageImpl inteira e avisa no log que
// esse formato pode mudar entre versões.
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class WebConfig {
}
