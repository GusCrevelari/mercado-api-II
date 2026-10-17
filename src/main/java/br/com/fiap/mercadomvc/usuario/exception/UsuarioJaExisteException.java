package br.com.fiap.mercadomvc.usuario.exception;

public class UsuarioJaExisteException extends RuntimeException {

    public UsuarioJaExisteException() {
        super("Este usuario ja esta cadastrado.");
    }
}
