package br.com.fiap.mercadomvc.api.dto;

import java.math.BigDecimal;

public record MercadoResponse(
        Long id,
        String nome,
        String tipo,
        String setor,
        String tamanho,
        BigDecimal preco) {
}
