package br.com.fiap.mercadomvc.compra.pagamento.outbox;

import br.com.fiap.mercadomvc.compra.model.Compra;
import br.com.fiap.mercadomvc.compra.pagamento.dto.PaymentRequest;
import br.com.fiap.mercadomvc.compra.pagamento.messaging.PaymentRequestEnvelope;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentOutboxDispatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentOutboxDispatchService.class);

    private final PaymentOutboxRepository repository;
    private final PaymentRequestPublisher publisher;

    public PaymentOutboxDispatchService(
            PaymentOutboxRepository repository,
            PaymentRequestPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @Transactional
    public void dispatch(Long outboxId) {
        PaymentOutbox outbox = repository.findById(outboxId).orElse(null);
        if (outbox == null || outbox.getStatus() != PaymentOutboxStatus.PENDING) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        try {
            Compra compra = outbox.getCompra();
            PaymentRequest payload = new PaymentRequest(
                    compra.getId(),
                    compra.getCorrelationId(),
                    compra.getTotal(),
                    compra.getMoeda());
            publisher.publish(new PaymentRequestEnvelope(outbox.getPaymentAttemptId(), payload));
            outbox.registrarPublicacao(now);
            LOGGER.info(
                    "Solicitacao de pagamento publicada: compraId={}, paymentAttemptId={}, correlationId={}",
                    compra.getId(),
                    outbox.getPaymentAttemptId(),
                    compra.getCorrelationId());
        } catch (RuntimeException exception) {
            outbox.registrarFalha(now, exception.getClass().getSimpleName() + ": " + exception.getMessage());
            LOGGER.warn(
                    "Falha ao publicar solicitacao de pagamento: outboxId={}, paymentAttemptId={}, tentativa={}",
                    outbox.getId(),
                    outbox.getPaymentAttemptId(),
                    outbox.getTentativas(),
                    exception);
        }
    }
}
