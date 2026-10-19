// Define o pacote da camada de serviço.
package api.service;

// CAMADA: Service
// RESPONSABILIDADE: avisar que um fornecedor não existe. Herda de
//                   RecursoNaoEncontradoException, por isso vira 404.
// SITUAÇÃO: implementada.
public class FornecedorNaoEncontradoException extends RecursoNaoEncontradoException {

    // Cria a exceção usando o id que não foi encontrado.
    public FornecedorNaoEncontradoException(Long id) {
        // Envia a mensagem que vai aparecer no campo "mensagem" do ErroDTO.
        super("Fornecedor não encontrado: " + id);
    }
}
