package br.com.fiap.mercadomvc.dashboard.dto;

public record DashboardInventoryBar(
        Long produtoId,
        String label,
        int quantidade,
        int y,
        int largura,
        int valorX) {
}
