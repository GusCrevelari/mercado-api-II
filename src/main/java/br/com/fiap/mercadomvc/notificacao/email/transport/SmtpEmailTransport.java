package br.com.fiap.mercadomvc.notificacao.email.transport;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.notification.email.transport",
        havingValue = "smtp",
        matchIfMissing = true)
public class SmtpEmailTransport implements EmailTransport {

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpEmailTransport(
            JavaMailSender mailSender,
            @org.springframework.beans.factory.annotation.Value(
                    "${app.notification.email.from:no-reply@mercado.local}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(EmailMessage message) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(message.recipient());
        mail.setSubject(message.subject());
        mail.setText(message.body());
        mailSender.send(mail);
    }
}
