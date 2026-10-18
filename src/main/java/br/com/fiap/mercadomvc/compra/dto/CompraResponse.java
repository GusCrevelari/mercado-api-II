package br.com.fiap.mercadomvc.compra.dto;

import br.com.fiap.mercadomvc.compra.model.CompraStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CompraResponse(
        Long id,
        Long clienteId,
        String clienteLogin,
        CompraStatus status,
        BigDecimal total,
        String moeda,
        UUID correlationId,
        OffsetDateTime criadoEm,
        OffsetDateTime finalizadoEm,
        List<CompraItemResponse> itens) {
}
