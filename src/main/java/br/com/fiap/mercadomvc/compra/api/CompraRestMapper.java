package br.com.fiap.mercadomvc.compra.api;

import br.com.fiap.mercadomvc.compra.dto.CompraItemResponse;
import br.com.fiap.mercadomvc.compra.dto.CompraResponse;
import br.com.fiap.mercadomvc.compra.model.Compra;
import org.springframework.stereotype.Component;

@Component
public class CompraRestMapper {

    public CompraResponse toResponse(Compra compra) {
        return new CompraResponse(
                compra.getId(),
                compra.getCliente().getId(),
                compra.getCliente().getLogin(),
                compra.getStatus(),
                compra.getTotal(),
                compra.getMoeda(),
                compra.getCorrelationId(),
                compra.getCriadoEm(),
                compra.getFinalizadoEm(),
                compra.getItens().stream()
                        .map(item -> new CompraItemResponse(
                                item.getProduto().getId(),
                                item.getProdutoNome(),
                                item.getQuantidade(),
                                item.getPrecoUnitario(),
                                item.getSubtotal()))
                        .toList());
    }
}
