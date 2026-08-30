package br.com.fiap.mercadomvc.repository;

import br.com.fiap.mercadomvc.model.Mercado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MercadoRepository extends JpaRepository<Mercado, Long> {
}
