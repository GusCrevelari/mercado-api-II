package br.com.fiap.mercadomvc.compra.pagamento.outbox;

import br.com.fiap.mercadomvc.compra.model.Compra;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CP5_PAYMENT_OUTBOX")
@Getter
@NoArgsConstructor
public class PaymentOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cp5_seq_payment_outbox")
    @SequenceGenerator(
            name = "cp5_seq_payment_outbox",
            sequenceName = "CP5_SEQ_PAYMENT_OUTBOX",
            allocationSize = 1)
    @Column(name = "ID", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "COMPRA_ID", nullable = false, unique = true)
    private Compra compra;

    @Column(name = "PAYMENT_ATTEMPT_ID", nullable = false, unique = true)
    private UUID paymentAttemptId;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private PaymentOutboxStatus status;

    @Column(name = "TENTATIVAS", nullable = false)
    private int tentativas;

    @Column(name = "CRIADO_EM", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "ULTIMA_TENTATIVA_EM")
    private OffsetDateTime ultimaTentativaEm;

    @Column(name = "PUBLICADO_EM")
    private OffsetDateTime publicadoEm;

    @Column(name = "ULTIMO_ERRO", length = 1000)
    private String ultimoErro;

    public PaymentOutbox(Compra compra, UUID paymentAttemptId, OffsetDateTime criadoEm) {
        this.compra = compra;
        this.paymentAttemptId = paymentAttemptId;
        this.status = PaymentOutboxStatus.PENDING;
        this.tentativas = 0;
        this.criadoEm = criadoEm;
    }

    public void registrarPublicacao(OffsetDateTime momento) {
        tentativas++;
        ultimaTentativaEm = momento;
        publicadoEm = momento;
        ultimoErro = null;
        status = PaymentOutboxStatus.PUBLISHED;
    }

    public void registrarFalha(OffsetDateTime momento, String erro) {
        tentativas++;
        ultimaTentativaEm = momento;
        ultimoErro = limitar(erro);
    }

    private String limitar(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
