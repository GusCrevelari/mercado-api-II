package br.com.fiap.mercadomvc.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardView(
        BigDecimal receitaTotal,
        BigDecimal lucroTotal,
        long comprasAprovadas,
        long comprasPendentes,
        long comprasRejeitadas,
        List<DashboardInventoryRow> inventario,
        List<DashboardPurchaseRow> compras,
        List<DashboardFinancialBar> graficoFinanceiro,
        List<DashboardInventoryBar> graficoEstoque,
        int alturaGraficoEstoque) {

    public DashboardView {
        inventario = List.copyOf(inventario);
        compras = List.copyOf(compras);
        graficoFinanceiro = List.copyOf(graficoFinanceiro);
        graficoEstoque = List.copyOf(graficoEstoque);
    }
}
