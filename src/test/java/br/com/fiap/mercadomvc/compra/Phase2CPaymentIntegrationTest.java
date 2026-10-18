package br.com.fiap.mercadomvc.compra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import br.com.fiap.mercadomvc.compra.dto.CompraCreateRequest;
import br.com.fiap.mercadomvc.compra.dto.CompraItemRequest;
import br.com.fiap.mercadomvc.compra.dto.CompraResponse;
import br.com.fiap.mercadomvc.compra.exception.ResultadoPagamentoInvalidoException;
import br.com.fiap.mercadomvc.compra.model.CompraStatus;
import br.com.fiap.mercadomvc.compra.pagamento.PaymentProcessingOutcome;
import br.com.fiap.mercadomvc.compra.pagamento.PaymentStatus;
import br.com.fiap.mercadomvc.compra.pagamento.dto.PaymentResult;
import br.com.fiap.mercadomvc.compra.pagamento.messaging.PaymentRequestEnvelope;
import br.com.fiap.mercadomvc.compra.pagamento.messaging.PaymentResultEnvelope;
import br.com.fiap.mercadomvc.compra.pagamento.messaging.PaymentResultProcessingService;
import br.com.fiap.mercadomvc.compra.pagamento.outbox.PaymentOutbox;
import br.com.fiap.mercadomvc.compra.pagamento.outbox.PaymentOutboxDispatchService;
import br.com.fiap.mercadomvc.compra.pagamento.outbox.PaymentOutboxRepository;
import br.com.fiap.mercadomvc.compra.pagamento.outbox.PaymentOutboxStatus;
import br.com.fiap.mercadomvc.compra.pagamento.outbox.PaymentRequestPublisher;
import br.com.fiap.mercadomvc.compra.repository.CompraRepository;
import br.com.fiap.mercadomvc.compra.service.CompraService;
import br.com.fiap.mercadomvc.model.Mercado;
import br.com.fiap.mercadomvc.notificacao.email.EmailDeliveryStatus;
import br.com.fiap.mercadomvc.notificacao.email.EmailNotification;
import br.com.fiap.mercadomvc.notificacao.email.EmailNotificationDeliveryService;
import br.com.fiap.mercadomvc.notificacao.email.EmailNotificationRepository;
import br.com.fiap.mercadomvc.notificacao.email.EmailNotificationType;
import br.com.fiap.mercadomvc.notificacao.email.transport.EmailMessage;
import br.com.fiap.mercadomvc.notificacao.email.transport.EmailTransport;
import br.com.fiap.mercadomvc.repository.MercadoRepository;
import br.com.fiap.mercadomvc.usuario.model.PerfilUsuario;
import br.com.fiap.mercadomvc.usuario.model.Usuario;
import br.com.fiap.mercadomvc.usuario.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles({"postgres", "test"})
class Phase2CPaymentIntegrationTest {

    @Autowired
    private CompraService compraService;

    @Autowired
    private PaymentResultProcessingService resultProcessingService;

    @Autowired
    private PaymentOutboxDispatchService outboxDispatchService;

    @Autowired
    private EmailNotificationDeliveryService emailDeliveryService;

    @Autowired
    private PaymentOutboxRepository outboxRepository;

    @Autowired
    private EmailNotificationRepository notificationRepository;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private MercadoRepository mercadoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentRequestPublisher paymentRequestPublisher;

    @MockitoBean
    private EmailTransport emailTransport;

    @BeforeEach
    void cleanDatabaseAndMocks() {
        notificationRepository.deleteAll();
        outboxRepository.deleteAll();
        compraRepository.deleteAll();
        mercadoRepository.deleteAll();
        usuarioRepository.deleteAll();
        reset(paymentRequestPublisher, emailTransport);
    }

    @Test
    void purchaseAndOutboxArePersistedTogetherAndPublishCompatibleEnvelope() throws Exception {
        PurchaseFixture fixture = createPurchase(11L, "customer@example.test", 10, 2);
        PaymentOutbox outbox = outboxRepository.findByCompraId(fixture.purchase().id()).orElseThrow();

        assertThat(outbox.getStatus()).isEqualTo(PaymentOutboxStatus.PENDING);
        assertThat(outbox.getPaymentAttemptId()).isNotNull();

        outboxDispatchService.dispatch(outbox.getId());

        ArgumentCaptor<PaymentRequestEnvelope> captor = ArgumentCaptor.forClass(PaymentRequestEnvelope.class);
        verify(paymentRequestPublisher).publish(captor.capture());
        PaymentRequestEnvelope envelope = captor.getValue();
        assertThat(envelope.paymentAttemptId()).isEqualTo(outbox.getPaymentAttemptId());
        assertThat(envelope.payload().purchaseId()).isEqualTo(fixture.purchase().id());
        assertThat(envelope.payload().correlationId()).isEqualTo(fixture.purchase().correlationId());
        assertThat(envelope.payload().amount()).isEqualByComparingTo(fixture.purchase().total());
        assertThat(envelope.payload().currency()).isEqualTo("BRL");
        assertThat(objectMapper.writeValueAsString(envelope))
                .contains("paymentAttemptId", "payload", "purchaseId", "correlationId", "amount", "currency");

        PaymentOutbox published = outboxRepository.findById(outbox.getId()).orElseThrow();
        assertThat(published.getStatus()).isEqualTo(PaymentOutboxStatus.PUBLISHED);
        assertThat(published.getTentativas()).isEqualTo(1);
    }

    @Test
    void brokerFailureKeepsRecoverablePendingOutbox() {
        PurchaseFixture fixture = createPurchase(11L, "customer@example.test", 10, 1);
        PaymentOutbox outbox = outboxRepository.findByCompraId(fixture.purchase().id()).orElseThrow();
        doThrow(new IllegalStateException("broker offline"))
                .when(paymentRequestPublisher).publish(any(PaymentRequestEnvelope.class));

        outboxDispatchService.dispatch(outbox.getId());

        PaymentOutbox failed = outboxRepository.findById(outbox.getId()).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(PaymentOutboxStatus.PENDING);
        assertThat(failed.getTentativas()).isEqualTo(1);
        assertThat(failed.getUltimoErro()).contains("broker offline");
    }

    @Test
    void approvedResultKeepsReservedStockAndCreatesOneEmailNotification() {
        PurchaseFixture fixture = createPurchase(11L, "customer@example.test", 10, 3);
        PaymentResultEnvelope approved = result(fixture.purchase(), PaymentStatus.APPROVED);

        assertThat(resultProcessingService.process(approved)).isEqualTo(PaymentProcessingOutcome.APPLIED);
        assertThat(resultProcessingService.process(approved)).isEqualTo(PaymentProcessingOutcome.DUPLICATE_IGNORED);

        assertThat(compraRepository.findById(fixture.purchase().id()).orElseThrow().getStatus())
                .isEqualTo(CompraStatus.APPROVED);
        assertThat(mercadoRepository.findById(fixture.product().getId()).orElseThrow().getQuantidadeEstoque())
                .isEqualTo(7);
        assertThat(notificationRepository.countByCompraIdAndTipo(
                fixture.purchase().id(), EmailNotificationType.PAYMENT_APPROVED)).isEqualTo(1);
        EmailNotification notification = notificationRepository.findByCompraIdAndTipo(
                fixture.purchase().id(), EmailNotificationType.PAYMENT_APPROVED).orElseThrow();
        assertThat(notification.getDestinatario()).isEqualTo("customer@example.test");
        assertThat(notification.getStatus()).isEqualTo(EmailDeliveryStatus.PENDING);
    }

    @Test
    void rejectedResultRestoresStockAndDoesNotCreateEmail() {
        PurchaseFixture fixture = createPurchase(12L, "customer@example.test", 10, 3);

        assertThat(resultProcessingService.process(result(fixture.purchase(), PaymentStatus.REJECTED)))
                .isEqualTo(PaymentProcessingOutcome.APPLIED);

        assertThat(compraRepository.findById(fixture.purchase().id()).orElseThrow().getStatus())
                .isEqualTo(CompraStatus.REJECTED);
        assertThat(mercadoRepository.findById(fixture.product().getId()).orElseThrow().getQuantidadeEstoque())
                .isEqualTo(10);
        assertThat(notificationRepository.count()).isZero();
    }

    @Test
    void mismatchedAttemptIsRejectedWithoutChangingPurchase() {
        PurchaseFixture fixture = createPurchase(11L, "customer@example.test", 10, 2);
        PaymentResult payload = new PaymentResult(
                fixture.purchase().id(),
                fixture.purchase().correlationId(),
                fixture.purchase().total(),
                fixture.purchase().moeda(),
                PaymentStatus.APPROVED);

        assertThatThrownBy(() -> resultProcessingService.process(
                new PaymentResultEnvelope(java.util.UUID.randomUUID(), payload)))
                .isInstanceOf(ResultadoPagamentoInvalidoException.class);

        assertThat(compraRepository.findById(fixture.purchase().id()).orElseThrow().getStatus())
                .isEqualTo(CompraStatus.PENDING_PAYMENT);
        assertThat(notificationRepository.count()).isZero();
    }

    @Test
    void recordsEmailDeliverySuccess() {
        PurchaseFixture fixture = createPurchase(11L, "customer@example.test", 10, 1);
        resultProcessingService.process(result(fixture.purchase(), PaymentStatus.APPROVED));
        EmailNotification notification = notificationRepository.findByCompraIdAndTipo(
                fixture.purchase().id(), EmailNotificationType.PAYMENT_APPROVED).orElseThrow();

        emailDeliveryService.deliver(notification.getId());

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailTransport).send(captor.capture());
        assertThat(captor.getValue().recipient()).isEqualTo("customer@example.test");
        assertThat(captor.getValue().body()).contains(
                "compra #" + fixture.purchase().id(), fixture.purchase().correlationId().toString());
        EmailNotification delivered = notificationRepository.findById(notification.getId()).orElseThrow();
        assertThat(delivered.getStatus()).isEqualTo(EmailDeliveryStatus.SENT);
        assertThat(delivered.getTentativas()).isEqualTo(1);
        assertThat(delivered.getEnviadoEm()).isNotNull();
        assertThat(delivered.getUltimoErro()).isNull();
    }

    @Test
    void recordsEmailTransportFailureAndMissingLegacyEmail() {
        PurchaseFixture transportFailure = createPurchase(11L, "failure@example.test", 10, 1);
        resultProcessingService.process(result(transportFailure.purchase(), PaymentStatus.APPROVED));
        EmailNotification first = notificationRepository.findByCompraIdAndTipo(
                transportFailure.purchase().id(), EmailNotificationType.PAYMENT_APPROVED).orElseThrow();
        doThrow(new IllegalStateException("mail service offline"))
                .when(emailTransport).send(any(EmailMessage.class));

        emailDeliveryService.deliver(first.getId());

        EmailNotification failed = notificationRepository.findById(first.getId()).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(EmailDeliveryStatus.FAILED);
        assertThat(failed.getUltimoErro()).contains("mail service offline");

        reset(emailTransport);
        PurchaseFixture legacy = createPurchase(13L, null, 10, 1);
        resultProcessingService.process(result(legacy.purchase(), PaymentStatus.APPROVED));
        EmailNotification missingEmail = notificationRepository.findByCompraIdAndTipo(
                legacy.purchase().id(), EmailNotificationType.PAYMENT_APPROVED).orElseThrow();

        emailDeliveryService.deliver(missingEmail.getId());

        EmailNotification missingEmailFailed = notificationRepository.findById(missingEmail.getId()).orElseThrow();
        assertThat(missingEmailFailed.getStatus()).isEqualTo(EmailDeliveryStatus.FAILED);
        assertThat(missingEmailFailed.getUltimoErro()).contains("Cliente sem email cadastrado");
    }

    private PaymentResultEnvelope result(CompraResponse purchase, PaymentStatus status) {
        PaymentOutbox outbox = outboxRepository.findByCompraId(purchase.id()).orElseThrow();
        return new PaymentResultEnvelope(
                outbox.getPaymentAttemptId(),
                new PaymentResult(
                        purchase.id(), purchase.correlationId(), purchase.total(), purchase.moeda(), status));
    }

    private PurchaseFixture createPurchase(long suffix, String email, int stock, int quantity) {
        String login = "customer." + suffix;
        usuarioRepository.save(Usuario.builder()
                .nome("Customer " + suffix)
                .login(login)
                .email(email)
                .senha(passwordEncoder.encode("secure-password"))
                .ativo(true)
                .perfil(PerfilUsuario.CLIENTE)
                .build());
        Mercado product = mercadoRepository.save(Mercado.builder()
                .nome("Product " + suffix)
                .tipo("Food")
                .setor("Grocery")
                .tamanho("1 unit")
                .preco(new BigDecimal("12.50"))
                .custoAquisicao(new BigDecimal("7.25"))
                .quantidadeEstoque(stock)
                .build());
        CompraResponse purchase = compraService.criar(
                login,
                new CompraCreateRequest(List.of(new CompraItemRequest(product.getId(), quantity))));
        return new PurchaseFixture(purchase, product);
    }

    private record PurchaseFixture(CompraResponse purchase, Mercado product) {
    }
}
