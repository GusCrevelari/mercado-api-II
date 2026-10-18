package br.com.fiap.mercadomvc.compra.pagamento.messaging;

import br.com.fiap.mercadomvc.compra.exception.CompraNaoEncontradaException;
import br.com.fiap.mercadomvc.compra.exception.ResultadoPagamentoInvalidoException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class PaymentResultListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentResultListener.class);

    private final ObjectMapper objectMapper;
    private final PaymentResultProcessingService processingService;
    private final PaymentResultDeadLetterPublisher deadLetterPublisher;

    public PaymentResultListener(
            ObjectMapper objectMapper,
            PaymentResultProcessingService processingService,
            PaymentResultDeadLetterPublisher deadLetterPublisher) {
        this.objectMapper = objectMapper;
        this.processingService = processingService;
        this.deadLetterPublisher = deadLetterPublisher;
    }

    @RabbitListener(queues = RabbitTopology.RESULT_QUEUE)
    public void receive(Message message) {
        try {
            PaymentResultEnvelope envelope = objectMapper.readValue(
                    message.getBody(), PaymentResultEnvelope.class);
            var outcome = processingService.process(envelope);
            LOGGER.info("Resultado do pagamento {} processado: {}", envelope.paymentAttemptId(), outcome);
        } catch (JacksonException
                | ResultadoPagamentoInvalidoException
                | CompraNaoEncontradaException
                | IllegalArgumentException invalidMessage) {
            deadLetterPublisher.publish(message, invalidMessage);
            LOGGER.warn("Resultado de pagamento invalido enviado para DLQ: {}", invalidMessage.getMessage());
        }
    }
}
