package br.com.fiap.mercadomvc.compra.pagamento.outbox;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, Long> {

    Optional<PaymentOutbox> findByCompraId(Long compraId);

    boolean existsByPaymentAttemptId(UUID paymentAttemptId);

    @Query("select o.id from PaymentOutbox o where o.status = :status order by o.criadoEm")
    List<Long> findIdsByStatus(@Param("status") PaymentOutboxStatus status);
}
