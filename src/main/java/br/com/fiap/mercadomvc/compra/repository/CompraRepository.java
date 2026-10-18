package br.com.fiap.mercadomvc.compra.repository;

import br.com.fiap.mercadomvc.compra.model.Compra;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    @EntityGraph(attributePaths = {"cliente", "itens", "itens.produto"})
    List<Compra> findAllByOrderByCriadoEmDesc();

    @EntityGraph(attributePaths = {"cliente", "itens", "itens.produto"})
    List<Compra> findAllByClienteLoginIgnoreCaseOrderByCriadoEmDesc(String login);

    @EntityGraph(attributePaths = {"cliente", "itens", "itens.produto"})
    @Query("select c from Compra c where c.id = :id")
    Optional<Compra> buscarPorIdComDetalhes(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Compra c where c.id = :id")
    Optional<Compra> buscarPorIdComBloqueio(@Param("id") Long id);
}
