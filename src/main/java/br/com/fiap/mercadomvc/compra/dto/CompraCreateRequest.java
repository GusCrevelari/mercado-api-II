package br.com.fiap.mercadomvc.compra.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CompraCreateRequest(
        @NotEmpty(message = "Informe ao menos um item.")
        List<@Valid CompraItemRequest> itens) {
}
