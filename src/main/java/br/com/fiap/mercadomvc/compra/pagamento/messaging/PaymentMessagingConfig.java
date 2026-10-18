package br.com.fiap.mercadomvc.compra.pagamento.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentMessagingConfig {

    @Bean
    DirectExchange paymentsExchange() {
        return ExchangeBuilder.directExchange(RabbitTopology.PAYMENTS_EXCHANGE).durable(true).build();
    }

    @Bean
    DirectExchange paymentDeadLetterExchange() {
        return ExchangeBuilder.directExchange(RabbitTopology.DEAD_LETTER_EXCHANGE).durable(true).build();
    }

    @Bean
    Queue paymentRequestQueue() {
        return QueueBuilder.durable(RabbitTopology.REQUEST_QUEUE)
                .deadLetterExchange(RabbitTopology.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(RabbitTopology.REQUEST_DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue paymentResultQueue() {
        return QueueBuilder.durable(RabbitTopology.RESULT_QUEUE).build();
    }

    @Bean
    Queue paymentResultDeadLetterQueue() {
        return QueueBuilder.durable(RabbitTopology.RESULT_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding paymentRequestBinding(Queue paymentRequestQueue, DirectExchange paymentsExchange) {
        return BindingBuilder.bind(paymentRequestQueue).to(paymentsExchange)
                .with(RabbitTopology.REQUEST_ROUTING_KEY);
    }

    @Bean
    Binding paymentResultBinding(Queue paymentResultQueue, DirectExchange paymentsExchange) {
        return BindingBuilder.bind(paymentResultQueue).to(paymentsExchange)
                .with(RabbitTopology.RESULT_ROUTING_KEY);
    }

    @Bean
    Binding paymentResultDeadLetterBinding(
            Queue paymentResultDeadLetterQueue,
            DirectExchange paymentDeadLetterExchange) {
        return BindingBuilder.bind(paymentResultDeadLetterQueue).to(paymentDeadLetterExchange)
                .with(RabbitTopology.RESULT_DEAD_LETTER_ROUTING_KEY);
    }
}
