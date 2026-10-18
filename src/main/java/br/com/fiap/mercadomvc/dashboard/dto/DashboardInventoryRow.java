package br.com.fiap.mercadomvc.dashboard.dto;

public record DashboardInventoryRow(
        Long id,
        String nome,
        String tipo,
        String setor,
        String tamanho,
        Integer quantidadeEstoque) {
}
