package br.com.fiap.mercadomvc.notificacao.email.transport;

public interface EmailTransport {

    void send(EmailMessage message);
}
