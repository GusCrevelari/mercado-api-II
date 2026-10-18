package br.com.fiap.mercadomvc.notificacao.email;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.notification.email.enabled", havingValue = "true", matchIfMissing = true)
public class EmailNotificationScheduler {

    private final EmailNotificationRepository repository;
    private final EmailNotificationDeliveryService deliveryService;
    private final int maxAttempts;

    public EmailNotificationScheduler(
            EmailNotificationRepository repository,
            EmailNotificationDeliveryService deliveryService,
            @Value("${app.notification.email.max-attempts:3}") int maxAttempts) {
        this.repository = repository;
        this.deliveryService = deliveryService;
        this.maxAttempts = maxAttempts;
    }

    @Scheduled(fixedDelayString = "${app.notification.email.delay:2s}")
    public void deliverPending() {
        repository.findDeliverableIds(
                        List.of(EmailDeliveryStatus.PENDING, EmailDeliveryStatus.FAILED),
                        maxAttempts)
                .stream()
                .limit(20)
                .forEach(deliveryService::deliver);
    }
}
