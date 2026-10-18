package br.com.fiap.mercadomvc.compra.pagamento.dto;

import br.com.fiap.mercadomvc.compra.pagamento.PaymentStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResult(
        Long purchaseId,
        UUID correlationId,
        BigDecimal amount,
        String currency,
        PaymentStatus status) {
}
