package br.com.fiap.mercadomvc.compra.repository;

import br.com.fiap.mercadomvc.compra.model.CompraItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompraItemRepository extends JpaRepository<CompraItem, Long> {
}
