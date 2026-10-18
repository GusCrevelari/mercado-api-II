package br.com.fiap.mercadomvc.compra.exception;

public class CompraAcessoNegadoException extends RuntimeException {

    public CompraAcessoNegadoException() {
        super("A compra pertence a outro cliente.");
    }
}
