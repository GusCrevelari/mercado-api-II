package br.com.fiap.mercadomvc.compra.pagamento.outbox;

import br.com.fiap.mercadomvc.compra.pagamento.messaging.PaymentRequestEnvelope;
import br.com.fiap.mercadomvc.compra.pagamento.messaging.RabbitTopology;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentRequestPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final Duration confirmTimeout;

    public PaymentRequestPublisher(
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            @Value("${app.payment.publisher-confirm-timeout:5s}") Duration confirmTimeout) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.confirmTimeout = confirmTimeout;
    }

    public void publish(PaymentRequestEnvelope envelope) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        properties.setMessageId(envelope.paymentAttemptId().toString());
        properties.setCorrelationId(envelope.payload().correlationId().toString());
        Message message = new Message(serialize(envelope), properties);
        CorrelationData correlation = new CorrelationData(envelope.paymentAttemptId().toString());
        rabbitTemplate.send(
                RabbitTopology.PAYMENTS_EXCHANGE,
                RabbitTopology.REQUEST_ROUTING_KEY,
                message,
                correlation);
        try {
            CorrelationData.Confirm confirm = correlation.getFuture()
                    .get(confirmTimeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!confirm.ack()) {
                throw new IllegalStateException("RabbitMQ rejeitou a publicacao: " + confirm.reason());
            }
            if (correlation.getReturned() != null) {
                throw new IllegalStateException("RabbitMQ nao encontrou fila para payment.request.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Confirmacao RabbitMQ interrompida.", exception);
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException exception) {
            throw new IllegalStateException("Confirmacao RabbitMQ indisponivel.", exception);
        }
    }

    private byte[] serialize(PaymentRequestEnvelope envelope) {
        try {
            return objectMapper.writeValueAsBytes(envelope);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Nao foi possivel serializar a solicitacao de pagamento.", exception);
        }
    }
}
