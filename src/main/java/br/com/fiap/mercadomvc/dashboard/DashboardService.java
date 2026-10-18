package br.com.fiap.mercadomvc.dashboard;

import br.com.fiap.mercadomvc.compra.model.Compra;
import br.com.fiap.mercadomvc.compra.model.CompraItem;
import br.com.fiap.mercadomvc.compra.model.CompraStatus;
import br.com.fiap.mercadomvc.compra.repository.CompraRepository;
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

        return new DashboardView(
                dinheiro(receita),
                dinheiro(lucro),
                aprovadas,
                pendentes,
                rejeitadas,
                inventario,
                linhasCompras);
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
