package br.com.fiap.mercadomvc.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "TDS_MVC_TB_MERCADO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mercado {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tds_mvc_seq_mercado")
    @SequenceGenerator(name = "tds_mvc_seq_mercado", sequenceName = "TDS_MVC_SEQ_MERCADO", allocationSize = 1)
    @Column(name = "ID", nullable = false)
    private Long id;

    @NotBlank(message = "Informe o nome do produto.")
    @Size(max = 120, message = "O nome deve ter no maximo 120 caracteres.")
    @Column(name = "NOME", nullable = false, length = 120)
    private String nome;

    @NotBlank(message = "Informe o tipo do produto.")
    @Size(max = 50, message = "O tipo deve ter no maximo 50 caracteres.")
    @Column(name = "TIPO", nullable = false, length = 50)
    private String tipo;

    @NotBlank(message = "Informe o setor do produto.")
    @Size(max = 80, message = "O setor deve ter no maximo 80 caracteres.")
    @Column(name = "SETOR", nullable = false, length = 80)
    private String setor;

    @NotBlank(message = "Informe o tamanho do produto.")
    @Size(max = 40, message = "O tamanho deve ter no maximo 40 caracteres.")
    @Column(name = "TAMANHO", nullable = false, length = 40)
    private String tamanho;

    @NotNull(message = "Informe o preco do produto.")
    @DecimalMin(value = "0.01", message = "O preco deve ser maior que zero.")
    @Digits(integer = 8, fraction = 2, message = "Use no maximo 8 digitos inteiros e 2 casas decimais.")
    @Column(name = "PRECO", nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;
}
