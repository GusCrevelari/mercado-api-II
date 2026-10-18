package br.com.fiap.mercadomvc.compra.model;

import br.com.fiap.mercadomvc.model.Mercado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CP5_COMPRA_ITEM")
@Getter
@NoArgsConstructor
public class CompraItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cp5_seq_compra_item")
    @SequenceGenerator(name = "cp5_seq_compra_item", sequenceName = "CP5_SEQ_COMPRA_ITEM", allocationSize = 1)
    @Column(name = "ID", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "COMPRA_ID", nullable = false)
    private Compra compra;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PRODUTO_ID", nullable = false)
    private Mercado produto;

    @Column(name = "PRODUTO_NOME", nullable = false, length = 120)
    private String produtoNome;

    @Column(name = "QUANTIDADE", nullable = false)
    private Integer quantidade;

    @Column(name = "PRECO_UNITARIO", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @Column(name = "CUSTO_UNITARIO", nullable = false, precision = 10, scale = 2)
    private BigDecimal custoUnitario;

    @Column(name = "SUBTOTAL", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "SUBTOTAL_CUSTO", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotalCusto;

    public CompraItem(
            Mercado produto,
            int quantidade,
            BigDecimal precoUnitario,
            BigDecimal custoUnitario,
            BigDecimal subtotal,
            BigDecimal subtotalCusto) {
        this.produto = produto;
        this.produtoNome = produto.getNome();
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.custoUnitario = custoUnitario;
        this.subtotal = subtotal;
        this.subtotalCusto = subtotalCusto;
    }

    void vincularA(Compra compra) {
        this.compra = compra;
    }
}
