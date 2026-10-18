package br.com.fiap.mercadomvc.notificacao.email;

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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CP5_EMAIL_NOTIFICATION")
@Getter
@NoArgsConstructor
public class EmailNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cp5_seq_email_notification")
    @SequenceGenerator(
            name = "cp5_seq_email_notification",
            sequenceName = "CP5_SEQ_EMAIL_NOTIFICATION",
            allocationSize = 1)
    @Column(name = "ID", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "COMPRA_ID", nullable = false)
    private Compra compra;

    @Column(name = "DESTINATARIO", length = 254)
    private String destinatario;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", nullable = false, length = 40)
    private EmailNotificationType tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private EmailDeliveryStatus status;

    @Column(name = "TENTATIVAS", nullable = false)
    private int tentativas;

    @Column(name = "CRIADO_EM", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "ULTIMA_TENTATIVA_EM")
    private OffsetDateTime ultimaTentativaEm;

    @Column(name = "ENVIADO_EM")
    private OffsetDateTime enviadoEm;

    @Column(name = "ULTIMO_ERRO", length = 1000)
    private String ultimoErro;

    public EmailNotification(Compra compra, String destinatario, OffsetDateTime criadoEm) {
        this.compra = compra;
        this.destinatario = destinatario;
        this.tipo = EmailNotificationType.PAYMENT_APPROVED;
        this.status = EmailDeliveryStatus.PENDING;
        this.tentativas = 0;
        this.criadoEm = criadoEm;
    }

    public void registrarSucesso(OffsetDateTime momento) {
        tentativas++;
        ultimaTentativaEm = momento;
        enviadoEm = momento;
        ultimoErro = null;
        status = EmailDeliveryStatus.SENT;
    }

    public void registrarFalha(OffsetDateTime momento, String erro) {
        tentativas++;
        ultimaTentativaEm = momento;
        ultimoErro = limitar(erro);
        status = EmailDeliveryStatus.FAILED;
    }

    private String limitar(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
