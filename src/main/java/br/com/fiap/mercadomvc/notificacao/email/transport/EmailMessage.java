package br.com.fiap.mercadomvc.notificacao.email.transport;

public record EmailMessage(
        String recipient,
        String subject,
        String body) {
}
