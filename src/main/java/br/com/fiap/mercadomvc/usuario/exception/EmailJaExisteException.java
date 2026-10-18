package br.com.fiap.mercadomvc.usuario.exception;

public class EmailJaExisteException extends RuntimeException {

    public EmailJaExisteException() {
        super("Este email ja esta cadastrado.");
    }
}
