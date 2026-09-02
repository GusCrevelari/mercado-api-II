package br.com.fiap.mercadomvc.api.dto;

import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import tools.jackson.databind.JsonNode;

public class MercadoPatchRequest {

    private final Map<String, JsonNode> campos;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public MercadoPatchRequest(Map<String, JsonNode> campos) {
        this.campos = campos == null ? Map.of() : Map.copyOf(campos);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return campos.isEmpty();
    }

    @JsonIgnore
    public boolean has(String campo) {
        return campos.containsKey(campo);
    }

    @JsonIgnore
    public JsonNode get(String campo) {
        return campos.get(campo);
    }

    @JsonIgnore
    public Set<String> fieldNames() {
        return campos.keySet();
    }
}
