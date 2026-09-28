package cm.gimac.des.document.repository;

import cm.gimac.des.document.domain.Consultation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Consultation entity.
 */
@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, UUID>, JpaSpecificationExecutor<Consultation> {
    default Optional<Consultation> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Consultation> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Consultation> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select consultation from Consultation consultation left join fetch consultation.document",
        countQuery = "select count(consultation) from Consultation consultation"
    )
    Page<Consultation> findAllWithToOneRelationships(Pageable pageable);

    @Query("select consultation from Consultation consultation left join fetch consultation.document")
    List<Consultation> findAllWithToOneRelationships();

    @Query("select consultation from Consultation consultation left join fetch consultation.document where consultation.id =:id")
    Optional<Consultation> findOneWithToOneRelationships(@Param("id") UUID id);
}
