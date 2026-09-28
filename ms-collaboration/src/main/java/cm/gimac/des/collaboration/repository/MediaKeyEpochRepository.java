package cm.gimac.des.collaboration.repository;

import cm.gimac.des.collaboration.domain.MediaKeyEpoch;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the MediaKeyEpoch entity.
 */
@Repository
public interface MediaKeyEpochRepository extends JpaRepository<MediaKeyEpoch, UUID> {
    default Optional<MediaKeyEpoch> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<MediaKeyEpoch> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<MediaKeyEpoch> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select mediaKeyEpoch from MediaKeyEpoch mediaKeyEpoch left join fetch mediaKeyEpoch.conference",
        countQuery = "select count(mediaKeyEpoch) from MediaKeyEpoch mediaKeyEpoch"
    )
    Page<MediaKeyEpoch> findAllWithToOneRelationships(Pageable pageable);

    @Query("select mediaKeyEpoch from MediaKeyEpoch mediaKeyEpoch left join fetch mediaKeyEpoch.conference")
    List<MediaKeyEpoch> findAllWithToOneRelationships();

    @Query("select mediaKeyEpoch from MediaKeyEpoch mediaKeyEpoch left join fetch mediaKeyEpoch.conference where mediaKeyEpoch.id =:id")
    Optional<MediaKeyEpoch> findOneWithToOneRelationships(@Param("id") UUID id);
}
