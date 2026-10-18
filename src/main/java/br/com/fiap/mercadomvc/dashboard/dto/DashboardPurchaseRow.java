package br.com.fiap.mercadomvc.dashboard.dto;

import java.math.BigDecimal;

public record DashboardPurchaseRow(
        Long id,
        String cliente,
        String data,
        BigDecimal total,
        String moeda,
        String status,
        String statusCssClass) {
}
