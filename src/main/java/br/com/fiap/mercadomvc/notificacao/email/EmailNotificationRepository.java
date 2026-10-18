package br.com.fiap.mercadomvc.notificacao.email;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailNotificationRepository extends JpaRepository<EmailNotification, Long> {

    Optional<EmailNotification> findByCompraIdAndTipo(Long compraId, EmailNotificationType tipo);

    long countByCompraIdAndTipo(Long compraId, EmailNotificationType tipo);

    @Query("""
            select n.id from EmailNotification n
            where n.status in :statuses and n.tentativas < :maxAttempts
            order by n.criadoEm
            """)
    List<Long> findDeliverableIds(
            @Param("statuses") Collection<EmailDeliveryStatus> statuses,
            @Param("maxAttempts") int maxAttempts);
}
