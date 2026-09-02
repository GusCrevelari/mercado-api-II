package br.com.fiap.mercadomvc.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record MercadoCreateRequest(
        @NotBlank(message = "Informe o nome do produto.")
        @Size(max = 120, message = "O nome deve ter no maximo 120 caracteres.")
        String nome,

        @NotBlank(message = "Informe o tipo do produto.")
        @Size(max = 50, message = "O tipo deve ter no maximo 50 caracteres.")
        String tipo,

        @NotBlank(message = "Informe o setor do produto.")
        @Size(max = 80, message = "O setor deve ter no maximo 80 caracteres.")
        String setor,

        @NotBlank(message = "Informe o tamanho do produto.")
        @Size(max = 40, message = "O tamanho deve ter no maximo 40 caracteres.")
        String tamanho,

        @NotNull(message = "Informe o preco do produto.")
        @DecimalMin(value = "0.01", message = "O preco deve ser maior que zero.")
        @Digits(integer = 8, fraction = 2, message = "Use no maximo 8 digitos inteiros e 2 casas decimais.")
        BigDecimal preco) {
}
