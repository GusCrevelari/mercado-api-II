package br.com.fiap.mercadomvc.compra.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CompraItemRequest(
        @NotNull(message = "Informe o produto.")
        @Positive(message = "O identificador do produto deve ser positivo.")
        Long produtoId,

        @NotNull(message = "Informe a quantidade.")
        @Positive(message = "A quantidade deve ser maior que zero.")
        Integer quantidade) {
}
