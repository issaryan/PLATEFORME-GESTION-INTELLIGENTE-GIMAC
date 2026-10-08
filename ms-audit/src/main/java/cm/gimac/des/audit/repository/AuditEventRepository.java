package cm.gimac.des.audit.repository;

import cm.gimac.des.audit.domain.AuditEvent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the AuditEvent entity.
 */
@SuppressWarnings("unused")
@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID>, JpaSpecificationExecutor<AuditEvent> {
    /** Index de la dernière feuille inscrite ; registre vide : null. */
    @Query("select max(a.logIndex) from AuditEvent a")
    Long dernierIndex();

    /**
     * Verrou transactionnel du registre (PostgreSQL, relâché à la fin de la transaction) : un seul inscripteur à la
     * fois, index sans trou ni doublon.
     */
    @Query(value = "select count(*) from (select pg_advisory_xact_lock(9162)) verrou", nativeQuery = true)
    long verrouillerRegistre();

    Optional<AuditEvent> findByEventId(UUID eventId);
}
