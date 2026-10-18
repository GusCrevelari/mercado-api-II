package br.com.fiap.mercadomvc.compra;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fiap.mercadomvc.compra.exception.ResultadoPagamentoInvalidoException;
import br.com.fiap.mercadomvc.compra.pagamento.messaging.PaymentResultDeadLetterPublisher;
import br.com.fiap.mercadomvc.compra.pagamento.messaging.PaymentResultEnvelope;
import br.com.fiap.mercadomvc.compra.pagamento.messaging.PaymentResultListener;
import br.com.fiap.mercadomvc.compra.pagamento.messaging.PaymentResultProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PaymentResultListenerTest {

    @Mock
    private PaymentResultProcessingService processingService;

    @Mock
    private PaymentResultDeadLetterPublisher deadLetterPublisher;

    private PaymentResultListener listener;

    @BeforeEach
    void setUp() {
        listener = new PaymentResultListener(
                new ObjectMapper(), processingService, deadLetterPublisher);
    }

    @Test
    void malformedJsonGoesToResultDeadLetterQueue() {
        Message source = new Message("{".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                new MessageProperties());

        listener.receive(source);

        verify(deadLetterPublisher).publish(
                org.mockito.ArgumentMatchers.eq(source),
                org.mockito.ArgumentMatchers.any(Exception.class));
        verify(processingService, never()).process(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unmatchedResultGoesToResultDeadLetterQueue() throws Exception {
        Message source = new Message("""
                {
                  "paymentAttemptId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                  "payload": {
                    "purchaseId": 99,
                    "correlationId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                    "amount": 10.00,
                    "currency": "BRL",
                    "status": "APPROVED"
                  }
                }
                """.getBytes(java.nio.charset.StandardCharsets.UTF_8), new MessageProperties());
        when(processingService.process(org.mockito.ArgumentMatchers.any(PaymentResultEnvelope.class)))
                .thenThrow(new ResultadoPagamentoInvalidoException("attempt not found"));

        listener.receive(source);

        verify(deadLetterPublisher).publish(
                org.mockito.ArgumentMatchers.eq(source),
                org.mockito.ArgumentMatchers.any(ResultadoPagamentoInvalidoException.class));
    }

    @Test
    void transientInfrastructureFailureIsRethrownForBrokerRedelivery() throws Exception {
        Message source = new Message("""
                {
                  "paymentAttemptId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                  "payload": {
                    "purchaseId": 99,
                    "correlationId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                    "amount": 10.00,
                    "currency": "BRL",
                    "status": "APPROVED"
                  }
                }
                """.getBytes(java.nio.charset.StandardCharsets.UTF_8), new MessageProperties());
        when(processingService.process(org.mockito.ArgumentMatchers.any(PaymentResultEnvelope.class)))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> listener.receive(source))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("database unavailable");
        verify(deadLetterPublisher, never()).publish(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
