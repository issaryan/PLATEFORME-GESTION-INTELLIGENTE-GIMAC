package cm.gimac.des.participant.repository;

import cm.gimac.des.participant.domain.LegalDocument;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the LegalDocument entity.
 */
@Repository
public interface LegalDocumentRepository extends JpaRepository<LegalDocument, UUID>, JpaSpecificationExecutor<LegalDocument> {
    default Optional<LegalDocument> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<LegalDocument> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<LegalDocument> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select legalDocument from LegalDocument legalDocument left join fetch legalDocument.previousDocument left join fetch legalDocument.institution",
        countQuery = "select count(legalDocument) from LegalDocument legalDocument"
    )
    Page<LegalDocument> findAllWithToOneRelationships(Pageable pageable);

    @Query(
        "select legalDocument from LegalDocument legalDocument left join fetch legalDocument.previousDocument left join fetch legalDocument.institution"
    )
    List<LegalDocument> findAllWithToOneRelationships();

    @Query(
        "select legalDocument from LegalDocument legalDocument left join fetch legalDocument.previousDocument left join fetch legalDocument.institution where legalDocument.id =:id"
    )
    Optional<LegalDocument> findOneWithToOneRelationships(@Param("id") UUID id);
}
