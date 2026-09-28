package cm.gimac.des.certification.repository;

import cm.gimac.des.certification.domain.EvidenceItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the EvidenceItem entity.
 */
@Repository
public interface EvidenceItemRepository extends JpaRepository<EvidenceItem, UUID>, JpaSpecificationExecutor<EvidenceItem> {
    default Optional<EvidenceItem> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<EvidenceItem> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<EvidenceItem> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select evidenceItem from EvidenceItem evidenceItem left join fetch evidenceItem.file",
        countQuery = "select count(evidenceItem) from EvidenceItem evidenceItem"
    )
    Page<EvidenceItem> findAllWithToOneRelationships(Pageable pageable);

    @Query("select evidenceItem from EvidenceItem evidenceItem left join fetch evidenceItem.file")
    List<EvidenceItem> findAllWithToOneRelationships();

    @Query("select evidenceItem from EvidenceItem evidenceItem left join fetch evidenceItem.file where evidenceItem.id =:id")
    Optional<EvidenceItem> findOneWithToOneRelationships(@Param("id") UUID id);
}
