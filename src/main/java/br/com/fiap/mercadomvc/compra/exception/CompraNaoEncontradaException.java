package br.com.fiap.mercadomvc.compra.exception;

public class CompraNaoEncontradaException extends RuntimeException {

    public CompraNaoEncontradaException(Long id) {
        super("Compra nao encontrada: " + id);
    }
}
