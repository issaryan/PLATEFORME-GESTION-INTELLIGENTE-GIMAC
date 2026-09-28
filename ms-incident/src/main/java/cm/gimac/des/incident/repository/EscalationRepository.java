package cm.gimac.des.incident.repository;

import cm.gimac.des.incident.domain.Escalation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Escalation entity.
 */
@Repository
public interface EscalationRepository extends JpaRepository<Escalation, UUID>, JpaSpecificationExecutor<Escalation> {
    default Optional<Escalation> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Escalation> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Escalation> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select escalation from Escalation escalation left join fetch escalation.ticket",
        countQuery = "select count(escalation) from Escalation escalation"
    )
    Page<Escalation> findAllWithToOneRelationships(Pageable pageable);

    @Query("select escalation from Escalation escalation left join fetch escalation.ticket")
    List<Escalation> findAllWithToOneRelationships();

    @Query("select escalation from Escalation escalation left join fetch escalation.ticket where escalation.id =:id")
    Optional<Escalation> findOneWithToOneRelationships(@Param("id") UUID id);
}
