package br.com.fiap.mercadomvc.notificacao.email;

import br.com.fiap.mercadomvc.compra.model.Compra;
import br.com.fiap.mercadomvc.notificacao.email.transport.EmailMessage;
import br.com.fiap.mercadomvc.notificacao.email.transport.EmailTransport;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EmailNotificationDeliveryService {

    private final EmailNotificationRepository repository;
    private final EmailTransport transport;

    public EmailNotificationDeliveryService(
            EmailNotificationRepository repository,
            EmailTransport transport) {
        this.repository = repository;
        this.transport = transport;
    }

    @Transactional
    public void deliver(Long notificationId) {
        EmailNotification notification = repository.findById(notificationId).orElse(null);
        if (notification == null || notification.getStatus() == EmailDeliveryStatus.SENT) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        try {
            if (!StringUtils.hasText(notification.getDestinatario())) {
                throw new IllegalStateException("Cliente sem email cadastrado.");
            }
            Compra compra = notification.getCompra();
            transport.send(new EmailMessage(
                    notification.getDestinatario(),
                    "Pagamento aprovado - compra #" + compra.getId(),
                    body(compra)));
            notification.registrarSucesso(now);
        } catch (RuntimeException exception) {
            notification.registrarFalha(now, exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private String body(Compra compra) {
        return """
                Ola, %s!

                O pagamento da compra #%d foi aprovado.
                Valor: %s %s
                Identificador: %s

                Mercado Express
                """.formatted(
                compra.getCliente().getNome(),
                compra.getId(),
                compra.getMoeda(),
                compra.getTotal().toPlainString(),
                compra.getCorrelationId());
    }
}
