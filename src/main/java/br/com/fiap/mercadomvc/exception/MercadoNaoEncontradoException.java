package br.com.fiap.mercadomvc.exception;

public class MercadoNaoEncontradoException extends RuntimeException {

    public MercadoNaoEncontradoException(Long id) {
        super("Produto nao encontrado: " + id);
    }
}
