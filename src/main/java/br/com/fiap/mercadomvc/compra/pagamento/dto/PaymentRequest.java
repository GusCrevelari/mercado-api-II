package br.com.fiap.mercadomvc.compra.pagamento.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(
        Long purchaseId,
        UUID correlationId,
        BigDecimal amount,
        String currency) {
}
