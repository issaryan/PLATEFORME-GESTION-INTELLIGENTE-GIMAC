package cm.gimac.des.collaboration.repository;

import cm.gimac.des.collaboration.domain.ConferenceParticipant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ConferenceParticipant entity.
 */
@Repository
public interface ConferenceParticipantRepository extends JpaRepository<ConferenceParticipant, UUID> {
    default Optional<ConferenceParticipant> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<ConferenceParticipant> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<ConferenceParticipant> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select conferenceParticipant from ConferenceParticipant conferenceParticipant left join fetch conferenceParticipant.conference",
        countQuery = "select count(conferenceParticipant) from ConferenceParticipant conferenceParticipant"
    )
    Page<ConferenceParticipant> findAllWithToOneRelationships(Pageable pageable);

    @Query("select conferenceParticipant from ConferenceParticipant conferenceParticipant left join fetch conferenceParticipant.conference")
    List<ConferenceParticipant> findAllWithToOneRelationships();

    @Query(
        "select conferenceParticipant from ConferenceParticipant conferenceParticipant left join fetch conferenceParticipant.conference where conferenceParticipant.id =:id"
    )
    Optional<ConferenceParticipant> findOneWithToOneRelationships(@Param("id") UUID id);
}
