package br.com.fiap.mercadomvc.compra.pagamento.messaging;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentResultDeadLetterPublisher {

    private final RabbitTemplate rabbitTemplate;

    public PaymentResultDeadLetterPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(Message source, Exception failure) {
        String error = failure.getClass().getSimpleName() + ": " + failure.getMessage();
        if (error.length() > 1000) {
            error = error.substring(0, 1000);
        }
        Message deadLetter = MessageBuilder.fromClonedMessage(source)
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setHeader("x-payment-result-error", error)
                .build();
        rabbitTemplate.send(
                RabbitTopology.DEAD_LETTER_EXCHANGE,
                RabbitTopology.RESULT_DEAD_LETTER_ROUTING_KEY,
                deadLetter);
    }
}
