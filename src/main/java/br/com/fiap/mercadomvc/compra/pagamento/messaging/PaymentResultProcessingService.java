package br.com.fiap.mercadomvc.compra.pagamento.messaging;

import br.com.fiap.mercadomvc.compra.exception.ResultadoPagamentoInvalidoException;
import br.com.fiap.mercadomvc.compra.model.Compra;
import br.com.fiap.mercadomvc.compra.pagamento.PaymentProcessingOutcome;
import br.com.fiap.mercadomvc.compra.pagamento.PaymentStatus;
import br.com.fiap.mercadomvc.compra.pagamento.outbox.PaymentOutbox;
import br.com.fiap.mercadomvc.compra.pagamento.outbox.PaymentOutboxRepository;
import br.com.fiap.mercadomvc.compra.repository.CompraRepository;
import br.com.fiap.mercadomvc.compra.service.CompraService;
import br.com.fiap.mercadomvc.notificacao.email.EmailNotification;
import br.com.fiap.mercadomvc.notificacao.email.EmailNotificationRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentResultProcessingService {

    private final PaymentOutboxRepository outboxRepository;
    private final CompraService compraService;
    private final CompraRepository compraRepository;
    private final EmailNotificationRepository notificationRepository;

    public PaymentResultProcessingService(
            PaymentOutboxRepository outboxRepository,
            CompraService compraService,
            CompraRepository compraRepository,
            EmailNotificationRepository notificationRepository) {
        this.outboxRepository = outboxRepository;
        this.compraService = compraService;
        this.compraRepository = compraRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public PaymentProcessingOutcome process(PaymentResultEnvelope envelope) {
        if (envelope == null || envelope.paymentAttemptId() == null || envelope.payload() == null) {
            throw new ResultadoPagamentoInvalidoException("Envelope de resultado incompleto.");
        }
        PaymentOutbox outbox = outboxRepository.findByCompraId(envelope.payload().purchaseId())
                .orElseThrow(() -> new ResultadoPagamentoInvalidoException(
                        "Nao existe tentativa de pagamento para a compra."));
        if (!outbox.getPaymentAttemptId().equals(envelope.paymentAttemptId())) {
            throw new ResultadoPagamentoInvalidoException("paymentAttemptId nao corresponde a compra.");
        }

        PaymentProcessingOutcome outcome = compraService.processarResultadoPagamento(envelope.payload());
        if (outcome == PaymentProcessingOutcome.APPLIED
                && envelope.payload().status() == PaymentStatus.APPROVED) {
            Compra compra = compraRepository.buscarPorIdComDetalhes(envelope.payload().purchaseId())
                    .orElseThrow();
            notificationRepository.save(new EmailNotification(
                    compra,
                    compra.getCliente().getEmail(),
                    OffsetDateTime.now(ZoneOffset.UTC)));
        }
        return outcome;
    }
}
