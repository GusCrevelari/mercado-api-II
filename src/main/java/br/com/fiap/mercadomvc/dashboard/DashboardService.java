package br.com.fiap.mercadomvc.dashboard;

import br.com.fiap.mercadomvc.compra.model.Compra;
import br.com.fiap.mercadomvc.compra.model.CompraItem;
import br.com.fiap.mercadomvc.compra.model.CompraStatus;
import br.com.fiap.mercadomvc.compra.repository.CompraRepository;
import br.com.fiap.mercadomvc.dashboard.dto.DashboardFinancialBar;
import br.com.fiap.mercadomvc.dashboard.dto.DashboardInventoryBar;
import br.com.fiap.mercadomvc.dashboard.dto.DashboardInventoryRow;
import br.com.fiap.mercadomvc.dashboard.dto.DashboardPurchaseRow;
import br.com.fiap.mercadomvc.dashboard.dto.DashboardView;
import br.com.fiap.mercadomvc.model.Mercado;
import br.com.fiap.mercadomvc.repository.MercadoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private static final BigDecimal ZERO_MONETARIO = new BigDecimal("0.00");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int EIXO_ZERO_FINANCEIRO = 440;
    private static final int LARGURA_MAXIMA_FINANCEIRO = 300;
    private static final int LARGURA_MAXIMA_ESTOQUE = 470;

    private final CompraRepository compraRepository;
    private final MercadoRepository mercadoRepository;

    public DashboardService(
            CompraRepository compraRepository,
            MercadoRepository mercadoRepository) {
        this.compraRepository = compraRepository;
        this.mercadoRepository = mercadoRepository;
    }

    @Transactional(readOnly = true)
    public DashboardView carregar() {
        List<Compra> compras = compraRepository.findAllByOrderByCriadoEmDesc();

        BigDecimal receita = ZERO_MONETARIO;
        BigDecimal lucro = ZERO_MONETARIO;
        long aprovadas = 0;
        long pendentes = 0;
        long rejeitadas = 0;

        for (Compra compra : compras) {
            switch (compra.getStatus()) {
                case APPROVED -> {
                    aprovadas++;
                    receita = receita.add(compra.getTotal());
                    lucro = lucro.add(calcularLucroPelosSnapshots(compra));
                }
                case PENDING_PAYMENT -> pendentes++;
                case REJECTED -> rejeitadas++;
            }
        }

        List<DashboardInventoryRow> inventario = mercadoRepository
                .findAll(Sort.by(Sort.Direction.ASC, "nome"))
                .stream()
                .map(this::toInventoryRow)
                .toList();

        List<DashboardPurchaseRow> linhasCompras = compras.stream()
                .map(this::toPurchaseRow)
                .toList();

        List<DashboardFinancialBar> graficoFinanceiro = aprovadas == 0
                ? List.of()
                : criarGraficoFinanceiro(dinheiro(receita), dinheiro(lucro));
        List<DashboardInventoryBar> graficoEstoque = criarGraficoEstoque(inventario);
        int alturaGraficoEstoque = Math.max(120, 44 + (graficoEstoque.size() * 44));

        return new DashboardView(
                dinheiro(receita),
                dinheiro(lucro),
                aprovadas,
                pendentes,
                rejeitadas,
                inventario,
                linhasCompras,
                graficoFinanceiro,
                graficoEstoque,
                alturaGraficoEstoque);
    }

    private List<DashboardFinancialBar> criarGraficoFinanceiro(BigDecimal receita, BigDecimal lucro) {
        BigDecimal maiorMagnitude = receita.abs().max(lucro.abs());
        if (maiorMagnitude.signum() == 0) {
            maiorMagnitude = BigDecimal.ONE;
        }
        return List.of(
                criarBarraFinanceira("Receita", receita, 42, "chart-bar-revenue", maiorMagnitude),
                criarBarraFinanceira("Lucro", lucro, 112, "chart-bar-profit", maiorMagnitude));
    }

    private DashboardFinancialBar criarBarraFinanceira(
            String label,
            BigDecimal valor,
            int y,
            String cssClass,
            BigDecimal maiorMagnitude) {
        int largura = larguraProporcional(valor.abs(), maiorMagnitude, LARGURA_MAXIMA_FINANCEIRO);
        int x = valor.signum() < 0 ? EIXO_ZERO_FINANCEIRO - largura : EIXO_ZERO_FINANCEIRO;
        int valorX = valor.signum() < 0 ? x - 10 : x + largura + 10;
        String valorAnchor = valor.signum() < 0 ? "end" : "start";
        return new DashboardFinancialBar(
                label,
                valor,
                x,
                y,
                largura,
                valorX,
                valorAnchor,
                cssClass);
    }

    private List<DashboardInventoryBar> criarGraficoEstoque(List<DashboardInventoryRow> inventario) {
        int maiorEstoque = inventario.stream()
                .mapToInt(DashboardInventoryRow::quantidadeEstoque)
                .max()
                .orElse(0);
        return java.util.stream.IntStream.range(0, inventario.size())
                .mapToObj(indice -> {
                    DashboardInventoryRow produto = inventario.get(indice);
                    int largura = larguraProporcional(
                            BigDecimal.valueOf(produto.quantidadeEstoque()),
                            BigDecimal.valueOf(Math.max(1, maiorEstoque)),
                            LARGURA_MAXIMA_ESTOQUE);
                    return new DashboardInventoryBar(
                            produto.id(),
                            abreviar(produto.nome(), 28),
                            produto.quantidadeEstoque(),
                            34 + (indice * 44),
                            largura,
                            270 + largura + 10);
                })
                .toList();
    }

    private int larguraProporcional(BigDecimal valor, BigDecimal maximo, int larguraMaxima) {
        if (valor.signum() == 0 || maximo.signum() == 0) {
            return 0;
        }
        int largura = valor
                .multiply(BigDecimal.valueOf(larguraMaxima))
                .divide(maximo, 0, RoundingMode.HALF_UP)
                .intValue();
        return Math.max(2, largura);
    }

    private String abreviar(String valor, int limite) {
        return valor.length() <= limite ? valor : valor.substring(0, limite - 3) + "...";
    }

    private BigDecimal calcularLucroPelosSnapshots(Compra compra) {
        return compra.getItens().stream()
                .map(this::margemDoItem)
                .reduce(ZERO_MONETARIO, BigDecimal::add);
    }

    private BigDecimal margemDoItem(CompraItem item) {
        BigDecimal margemUnitaria = item.getPrecoUnitario().subtract(item.getCustoUnitario());
        return margemUnitaria.multiply(BigDecimal.valueOf(item.getQuantidade()));
    }

    private DashboardInventoryRow toInventoryRow(Mercado produto) {
        return new DashboardInventoryRow(
                produto.getId(),
                produto.getNome(),
                produto.getTipo(),
                produto.getSetor(),
                produto.getTamanho(),
                produto.getQuantidadeEstoque());
    }

    private DashboardPurchaseRow toPurchaseRow(Compra compra) {
        return new DashboardPurchaseRow(
                compra.getId(),
                compra.getCliente().getNome() + " (" + compra.getCliente().getLogin() + ")",
                DATA_HORA.format(compra.getCriadoEm()),
                dinheiro(compra.getTotal()),
                compra.getMoeda(),
                statusLabel(compra.getStatus()),
                statusCssClass(compra.getStatus()));
    }

    private String statusLabel(CompraStatus status) {
        return switch (status) {
            case APPROVED -> "Aprovada";
            case PENDING_PAYMENT -> "Pendente";
            case REJECTED -> "Rejeitada";
        };
    }

    private String statusCssClass(CompraStatus status) {
        return switch (status) {
            case APPROVED -> "status-approved";
            case PENDING_PAYMENT -> "status-pending";
            case REJECTED -> "status-rejected";
        };
    }

    private BigDecimal dinheiro(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
