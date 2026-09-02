package br.com.fiap.mercadomvc.api;

import br.com.fiap.mercadomvc.api.dto.MercadoCreateRequest;
import br.com.fiap.mercadomvc.api.dto.MercadoPatchRequest;
import br.com.fiap.mercadomvc.api.dto.MercadoResponse;
import br.com.fiap.mercadomvc.api.dto.MercadoUpdateRequest;
import br.com.fiap.mercadomvc.api.exception.ApiValidationException;
import br.com.fiap.mercadomvc.model.Mercado;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
public class MercadoRestMapper {

    private static final Set<String> PATCH_FIELDS = Set.of("nome", "tipo", "setor", "tamanho", "preco");

    private final Validator validator;

    public Mercado toEntity(MercadoCreateRequest request) {
        return Mercado.builder()
                .nome(request.nome())
                .tipo(request.tipo())
                .setor(request.setor())
                .tamanho(request.tamanho())
                .preco(request.preco())
                .build();
    }

    public Mercado toEntity(MercadoUpdateRequest request) {
        return Mercado.builder()
                .nome(request.nome())
                .tipo(request.tipo())
                .setor(request.setor())
                .tamanho(request.tamanho())
                .preco(request.preco())
                .build();
    }

    public MercadoResponse toResponse(Mercado mercado) {
        return new MercadoResponse(
                mercado.getId(),
                mercado.getNome(),
                mercado.getTipo(),
                mercado.getSetor(),
                mercado.getTamanho(),
                mercado.getPreco());
    }

    public void applyPatch(MercadoPatchRequest request, Mercado mercado) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        if (request.isEmpty()) {
            throw new ApiValidationException("Informe ao menos um campo para atualizar.", fieldErrors);
        }

        request.fieldNames().stream()
                .filter(field -> !PATCH_FIELDS.contains(field))
                .forEach(field -> fieldErrors.put(field, "Campo nao permitido."));

        if (!fieldErrors.isEmpty()) {
            throw new ApiValidationException("Dados invalidos", fieldErrors);
        }

        if (request.has("nome")) {
            mercado.setNome(readText(request.get("nome"), "nome", fieldErrors));
        }
        if (request.has("tipo")) {
            mercado.setTipo(readText(request.get("tipo"), "tipo", fieldErrors));
        }
        if (request.has("setor")) {
            mercado.setSetor(readText(request.get("setor"), "setor", fieldErrors));
        }
        if (request.has("tamanho")) {
            mercado.setTamanho(readText(request.get("tamanho"), "tamanho", fieldErrors));
        }
        if (request.has("preco")) {
            mercado.setPreco(readBigDecimal(request.get("preco"), "preco", fieldErrors));
        }

        if (!fieldErrors.isEmpty()) {
            throw new ApiValidationException("Dados invalidos", fieldErrors);
        }

        validateEntity(mercado);
    }

    private String readText(JsonNode node, String field, Map<String, String> fieldErrors) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isString()) {
            fieldErrors.put(field, "Deve ser texto.");
            return null;
        }
        return node.asString();
    }

    private BigDecimal readBigDecimal(JsonNode node, String field, Map<String, String> fieldErrors) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber() || node.isString()) {
            try {
                return new BigDecimal(node.asString());
            } catch (NumberFormatException ex) {
                fieldErrors.put(field, "Deve ser um numero decimal valido.");
                return null;
            }
        }
        fieldErrors.put(field, "Deve ser um numero decimal valido.");
        return null;
    }

    private void validateEntity(Mercado mercado) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (ConstraintViolation<Mercado> violation : validator.validate(mercado)) {
            fieldErrors.put(violation.getPropertyPath().toString(), violation.getMessage());
        }
        if (!fieldErrors.isEmpty()) {
            throw new ApiValidationException("Dados invalidos", fieldErrors);
        }
    }
}
