package cm.gimac.des.monitoring.repository;

import cm.gimac.des.monitoring.domain.ReportSignature;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ReportSignature entity.
 */
@Repository
public interface ReportSignatureRepository extends JpaRepository<ReportSignature, UUID> {
    default Optional<ReportSignature> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<ReportSignature> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<ReportSignature> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select reportSignature from ReportSignature reportSignature left join fetch reportSignature.report",
        countQuery = "select count(reportSignature) from ReportSignature reportSignature"
    )
    Page<ReportSignature> findAllWithToOneRelationships(Pageable pageable);

    @Query("select reportSignature from ReportSignature reportSignature left join fetch reportSignature.report")
    List<ReportSignature> findAllWithToOneRelationships();

    @Query(
        "select reportSignature from ReportSignature reportSignature left join fetch reportSignature.report where reportSignature.id =:id"
    )
    Optional<ReportSignature> findOneWithToOneRelationships(@Param("id") UUID id);
}
