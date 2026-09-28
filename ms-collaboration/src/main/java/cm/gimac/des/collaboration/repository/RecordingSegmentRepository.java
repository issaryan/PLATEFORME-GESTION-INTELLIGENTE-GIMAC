package cm.gimac.des.collaboration.repository;

import cm.gimac.des.collaboration.domain.RecordingSegment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the RecordingSegment entity.
 */
@Repository
public interface RecordingSegmentRepository extends JpaRepository<RecordingSegment, UUID> {
    default Optional<RecordingSegment> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<RecordingSegment> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<RecordingSegment> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select recordingSegment from RecordingSegment recordingSegment left join fetch recordingSegment.conference",
        countQuery = "select count(recordingSegment) from RecordingSegment recordingSegment"
    )
    Page<RecordingSegment> findAllWithToOneRelationships(Pageable pageable);

    @Query("select recordingSegment from RecordingSegment recordingSegment left join fetch recordingSegment.conference")
    List<RecordingSegment> findAllWithToOneRelationships();

    @Query(
        "select recordingSegment from RecordingSegment recordingSegment left join fetch recordingSegment.conference where recordingSegment.id =:id"
    )
    Optional<RecordingSegment> findOneWithToOneRelationships(@Param("id") UUID id);
}
