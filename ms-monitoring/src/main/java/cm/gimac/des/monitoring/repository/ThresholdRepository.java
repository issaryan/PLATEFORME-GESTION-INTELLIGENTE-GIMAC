package cm.gimac.des.monitoring.repository;

import cm.gimac.des.monitoring.domain.Threshold;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Threshold entity.
 */
@Repository
public interface ThresholdRepository extends JpaRepository<Threshold, UUID> {
    default Optional<Threshold> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Threshold> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Threshold> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select threshold from Threshold threshold left join fetch threshold.link",
        countQuery = "select count(threshold) from Threshold threshold"
    )
    Page<Threshold> findAllWithToOneRelationships(Pageable pageable);

    @Query("select threshold from Threshold threshold left join fetch threshold.link")
    List<Threshold> findAllWithToOneRelationships();

    @Query("select threshold from Threshold threshold left join fetch threshold.link where threshold.id =:id")
    Optional<Threshold> findOneWithToOneRelationships(@Param("id") UUID id);
}
