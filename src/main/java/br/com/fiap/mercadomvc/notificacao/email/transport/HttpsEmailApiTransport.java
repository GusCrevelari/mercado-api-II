package br.com.fiap.mercadomvc.notificacao.email.transport;

import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(name = "app.notification.email.transport", havingValue = "https-api")
public class HttpsEmailApiTransport implements EmailTransport {

    private final HttpsEmailApiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public HttpsEmailApiTransport(HttpsEmailApiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(properties.getTimeout()).build();
    }

    @Override
    public void send(EmailMessage message) {
        if (properties.getEndpoint() == null || !StringUtils.hasText(properties.getToken())) {
            throw new IllegalStateException("EMAIL_API_URL e EMAIL_API_TOKEN nao foram configurados.");
        }
        if (!"https".equalsIgnoreCase(properties.getEndpoint().getScheme())) {
            throw new IllegalStateException("O transporte de email HTTP exige um endpoint HTTPS.");
        }
        try {
            byte[] body = objectMapper.writeValueAsBytes(new ApiEmailRequest(
                    properties.getFrom(), message.recipient(), message.subject(), message.body()));
            HttpRequest request = HttpRequest.newBuilder(properties.getEndpoint())
                    .timeout(properties.getTimeout())
                    .header("Authorization", "Bearer " + properties.getToken())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("API de email respondeu HTTP " + response.statusCode());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Envio de email interrompido.", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("API HTTPS de email indisponivel.", exception);
        }
    }

    private record ApiEmailRequest(String from, String to, String subject, String text) {
    }
}
