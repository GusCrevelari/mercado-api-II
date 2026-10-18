package br.com.fiap.mercadomvc.compra.pagamento.messaging;

import br.com.fiap.mercadomvc.compra.pagamento.dto.PaymentResult;
import java.util.UUID;

public record PaymentResultEnvelope(
        UUID paymentAttemptId,
        PaymentResult payload) {
}
