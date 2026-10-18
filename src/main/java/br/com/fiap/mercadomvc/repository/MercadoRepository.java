package br.com.fiap.mercadomvc.repository;

import br.com.fiap.mercadomvc.model.Mercado;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MercadoRepository extends JpaRepository<Mercado, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Mercado m where m.id = :id")
    Optional<Mercado> buscarPorIdComBloqueio(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Mercado m where m.id in :ids order by m.id")
    List<Mercado> buscarTodosPorIdComBloqueio(@Param("ids") Collection<Long> ids);
}
