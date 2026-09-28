package cm.gimac.des.pilotage.repository;

import cm.gimac.des.pilotage.domain.RiskAssessment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the RiskAssessment entity.
 */
@Repository
public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, UUID>, JpaSpecificationExecutor<RiskAssessment> {
    default Optional<RiskAssessment> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<RiskAssessment> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<RiskAssessment> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select riskAssessment from RiskAssessment riskAssessment left join fetch riskAssessment.risk",
        countQuery = "select count(riskAssessment) from RiskAssessment riskAssessment"
    )
    Page<RiskAssessment> findAllWithToOneRelationships(Pageable pageable);

    @Query("select riskAssessment from RiskAssessment riskAssessment left join fetch riskAssessment.risk")
    List<RiskAssessment> findAllWithToOneRelationships();

    @Query("select riskAssessment from RiskAssessment riskAssessment left join fetch riskAssessment.risk where riskAssessment.id =:id")
    Optional<RiskAssessment> findOneWithToOneRelationships(@Param("id") UUID id);
}
