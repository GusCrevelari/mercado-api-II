package br.com.fiap.mercadomvc.compra.pagamento.messaging;

import br.com.fiap.mercadomvc.compra.pagamento.dto.PaymentRequest;
import java.util.UUID;

public record PaymentRequestEnvelope(
        UUID paymentAttemptId,
        PaymentRequest payload) {
}
