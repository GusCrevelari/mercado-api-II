package br.com.fiap.mercadomvc.compra.dto;

import java.math.BigDecimal;

public record CompraItemResponse(
        Long produtoId,
        String produtoNome,
        Integer quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal) {
}
