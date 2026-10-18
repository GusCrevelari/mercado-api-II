package br.com.fiap.mercadomvc.compra.exception;

public class EstoqueInsuficienteException extends RuntimeException {

    public EstoqueInsuficienteException(Long produtoId, int solicitado, int disponivel) {
        super("Estoque insuficiente para o produto %d: solicitado %d, disponivel %d."
                .formatted(produtoId, solicitado, disponivel));
    }
}
