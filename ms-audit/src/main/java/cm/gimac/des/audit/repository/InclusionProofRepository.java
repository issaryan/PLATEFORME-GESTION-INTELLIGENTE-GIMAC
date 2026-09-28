package cm.gimac.des.audit.repository;

import cm.gimac.des.audit.domain.InclusionProof;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the InclusionProof entity.
 */
@Repository
public interface InclusionProofRepository extends JpaRepository<InclusionProof, UUID> {
    default Optional<InclusionProof> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<InclusionProof> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<InclusionProof> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select inclusionProof from InclusionProof inclusionProof left join fetch inclusionProof.periodRoot",
        countQuery = "select count(inclusionProof) from InclusionProof inclusionProof"
    )
    Page<InclusionProof> findAllWithToOneRelationships(Pageable pageable);

    @Query("select inclusionProof from InclusionProof inclusionProof left join fetch inclusionProof.periodRoot")
    List<InclusionProof> findAllWithToOneRelationships();

    @Query(
        "select inclusionProof from InclusionProof inclusionProof left join fetch inclusionProof.periodRoot where inclusionProof.id =:id"
    )
    Optional<InclusionProof> findOneWithToOneRelationships(@Param("id") UUID id);
}
