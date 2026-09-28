package cm.gimac.des.rapprochement.repository;

import cm.gimac.des.rapprochement.domain.RegularizationAttestation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the RegularizationAttestation entity.
 */
@Repository
public interface RegularizationAttestationRepository extends JpaRepository<RegularizationAttestation, UUID> {
    default Optional<RegularizationAttestation> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<RegularizationAttestation> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<RegularizationAttestation> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select regularizationAttestation from RegularizationAttestation regularizationAttestation left join fetch regularizationAttestation.suspens",
        countQuery = "select count(regularizationAttestation) from RegularizationAttestation regularizationAttestation"
    )
    Page<RegularizationAttestation> findAllWithToOneRelationships(Pageable pageable);

    @Query(
        "select regularizationAttestation from RegularizationAttestation regularizationAttestation left join fetch regularizationAttestation.suspens"
    )
    List<RegularizationAttestation> findAllWithToOneRelationships();

    @Query(
        "select regularizationAttestation from RegularizationAttestation regularizationAttestation left join fetch regularizationAttestation.suspens where regularizationAttestation.id =:id"
    )
    Optional<RegularizationAttestation> findOneWithToOneRelationships(@Param("id") UUID id);
}
