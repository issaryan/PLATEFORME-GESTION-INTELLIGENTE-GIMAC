package cm.gimac.des.collaboration.repository;

import cm.gimac.des.collaboration.domain.RecordingConsent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the RecordingConsent entity.
 */
@Repository
public interface RecordingConsentRepository extends JpaRepository<RecordingConsent, UUID> {
    default Optional<RecordingConsent> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<RecordingConsent> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<RecordingConsent> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select recordingConsent from RecordingConsent recordingConsent left join fetch recordingConsent.conference",
        countQuery = "select count(recordingConsent) from RecordingConsent recordingConsent"
    )
    Page<RecordingConsent> findAllWithToOneRelationships(Pageable pageable);

    @Query("select recordingConsent from RecordingConsent recordingConsent left join fetch recordingConsent.conference")
    List<RecordingConsent> findAllWithToOneRelationships();

    @Query(
        "select recordingConsent from RecordingConsent recordingConsent left join fetch recordingConsent.conference where recordingConsent.id =:id"
    )
    Optional<RecordingConsent> findOneWithToOneRelationships(@Param("id") UUID id);
}
