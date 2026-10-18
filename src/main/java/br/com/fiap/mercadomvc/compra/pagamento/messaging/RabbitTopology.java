package br.com.fiap.mercadomvc.compra.pagamento.messaging;

public final class RabbitTopology {

    public static final String PAYMENTS_EXCHANGE = "mercado.payments";
    public static final String DEAD_LETTER_EXCHANGE = "mercado.payments.dlx";
    public static final String REQUEST_QUEUE = "mercado.payment.requests";
    public static final String RESULT_QUEUE = "mercado.payment.results";
    public static final String RESULT_DEAD_LETTER_QUEUE = "mercado.payment.results.dlq";
    public static final String REQUEST_ROUTING_KEY = "payment.request";
    public static final String RESULT_ROUTING_KEY = "payment.result";
    public static final String REQUEST_DEAD_LETTER_ROUTING_KEY = "payment.request.dead";
    public static final String RESULT_DEAD_LETTER_ROUTING_KEY = "payment.result.dead";

    private RabbitTopology() {
    }
}
