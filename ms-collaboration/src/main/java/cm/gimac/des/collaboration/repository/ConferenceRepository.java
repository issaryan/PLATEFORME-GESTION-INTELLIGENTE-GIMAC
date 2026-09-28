package cm.gimac.des.collaboration.repository;

import cm.gimac.des.collaboration.domain.Conference;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Conference entity.
 */
@Repository
public interface ConferenceRepository extends JpaRepository<Conference, UUID>, JpaSpecificationExecutor<Conference> {
    default Optional<Conference> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Conference> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Conference> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select conference from Conference conference left join fetch conference.room",
        countQuery = "select count(conference) from Conference conference"
    )
    Page<Conference> findAllWithToOneRelationships(Pageable pageable);

    @Query("select conference from Conference conference left join fetch conference.room")
    List<Conference> findAllWithToOneRelationships();

    @Query("select conference from Conference conference left join fetch conference.room where conference.id =:id")
    Optional<Conference> findOneWithToOneRelationships(@Param("id") UUID id);
}
