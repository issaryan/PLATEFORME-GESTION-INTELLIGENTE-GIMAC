package cm.gimac.des.participant.repository;

import cm.gimac.des.participant.domain.EscalationContact;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the EscalationContact entity.
 */
@Repository
public interface EscalationContactRepository extends JpaRepository<EscalationContact, UUID>, JpaSpecificationExecutor<EscalationContact> {
    default Optional<EscalationContact> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<EscalationContact> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<EscalationContact> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select escalationContact from EscalationContact escalationContact left join fetch escalationContact.institution left join fetch escalationContact.correspondent left join fetch escalationContact.deputy",
        countQuery = "select count(escalationContact) from EscalationContact escalationContact"
    )
    Page<EscalationContact> findAllWithToOneRelationships(Pageable pageable);

    @Query(
        "select escalationContact from EscalationContact escalationContact left join fetch escalationContact.institution left join fetch escalationContact.correspondent left join fetch escalationContact.deputy"
    )
    List<EscalationContact> findAllWithToOneRelationships();

    @Query(
        "select escalationContact from EscalationContact escalationContact left join fetch escalationContact.institution left join fetch escalationContact.correspondent left join fetch escalationContact.deputy where escalationContact.id =:id"
    )
    Optional<EscalationContact> findOneWithToOneRelationships(@Param("id") UUID id);
}
