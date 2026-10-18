package br.com.fiap.mercadomvc.compra.model;

import br.com.fiap.mercadomvc.usuario.model.Usuario;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CP5_COMPRA")
@Getter
@NoArgsConstructor
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cp5_seq_compra")
    @SequenceGenerator(name = "cp5_seq_compra", sequenceName = "CP5_SEQ_COMPRA", allocationSize = 1)
    @Column(name = "ID", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USUARIO_ID", nullable = false)
    private Usuario cliente;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 30)
    private CompraStatus status;

    @Column(name = "TOTAL", nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Column(name = "CUSTO_TOTAL", nullable = false, precision = 14, scale = 2)
    private BigDecimal custoTotal;

    @Column(name = "MOEDA", nullable = false, length = 3)
    private String moeda;

    @Column(name = "CORRELATION_ID", nullable = false, unique = true)
    private UUID correlationId;

    @Column(name = "CRIADO_EM", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "ATUALIZADO_EM", nullable = false)
    private OffsetDateTime atualizadoEm;

    @Column(name = "FINALIZADO_EM")
    private OffsetDateTime finalizadoEm;

    @Version
    @Column(name = "VERSAO", nullable = false)
    private Long versao;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompraItem> itens = new ArrayList<>();

    public Compra(
            Usuario cliente,
            BigDecimal total,
            BigDecimal custoTotal,
            String moeda,
            UUID correlationId,
            OffsetDateTime criadoEm) {
        this.cliente = cliente;
        this.status = CompraStatus.PENDING_PAYMENT;
        this.total = total;
        this.custoTotal = custoTotal;
        this.moeda = moeda;
        this.correlationId = correlationId;
        this.criadoEm = criadoEm;
        this.atualizadoEm = criadoEm;
    }

    public List<CompraItem> getItens() {
        return Collections.unmodifiableList(itens);
    }

    public void adicionarItem(CompraItem item) {
        item.vincularA(this);
        itens.add(item);
    }

    public void aprovar(OffsetDateTime momento) {
        status = CompraStatus.APPROVED;
        atualizadoEm = momento;
        finalizadoEm = momento;
    }

    public void rejeitar(OffsetDateTime momento) {
        status = CompraStatus.REJECTED;
        atualizadoEm = momento;
        finalizadoEm = momento;
    }
}
