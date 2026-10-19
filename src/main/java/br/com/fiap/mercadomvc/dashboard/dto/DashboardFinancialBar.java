package br.com.fiap.mercadomvc.dashboard.dto;

import java.math.BigDecimal;

public record DashboardFinancialBar(
        String label,
        BigDecimal valor,
        int x,
        int y,
        int largura,
        int valorX,
        String valorAnchor,
        String cssClass) {
}
