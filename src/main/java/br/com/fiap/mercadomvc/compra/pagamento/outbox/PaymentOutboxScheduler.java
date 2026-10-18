package br.com.fiap.mercadomvc.compra.pagamento.outbox;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.payment.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentOutboxScheduler {

    private final PaymentOutboxRepository repository;
    private final PaymentOutboxDispatchService dispatchService;

    public PaymentOutboxScheduler(
            PaymentOutboxRepository repository,
            PaymentOutboxDispatchService dispatchService) {
        this.repository = repository;
        this.dispatchService = dispatchService;
    }

    @Scheduled(fixedDelayString = "${app.payment.outbox.delay:1s}")
    public void publishPending() {
        pendingIds().stream().limit(50).forEach(dispatchService::dispatch);
    }

    @Transactional(readOnly = true)
    List<Long> pendingIds() {
        return repository.findIdsByStatus(PaymentOutboxStatus.PENDING);
    }
}
