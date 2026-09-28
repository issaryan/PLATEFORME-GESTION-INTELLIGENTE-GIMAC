package cm.gimac.des.monitoring.repository;

import cm.gimac.des.monitoring.domain.ShiftReport;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ShiftReport entity.
 */
@Repository
public interface ShiftReportRepository extends JpaRepository<ShiftReport, UUID>, JpaSpecificationExecutor<ShiftReport> {
    default Optional<ShiftReport> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<ShiftReport> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<ShiftReport> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select shiftReport from ShiftReport shiftReport left join fetch shiftReport.shift left join fetch shiftReport.supersedes",
        countQuery = "select count(shiftReport) from ShiftReport shiftReport"
    )
    Page<ShiftReport> findAllWithToOneRelationships(Pageable pageable);

    @Query("select shiftReport from ShiftReport shiftReport left join fetch shiftReport.shift left join fetch shiftReport.supersedes")
    List<ShiftReport> findAllWithToOneRelationships();

    @Query(
        "select shiftReport from ShiftReport shiftReport left join fetch shiftReport.shift left join fetch shiftReport.supersedes where shiftReport.id =:id"
    )
    Optional<ShiftReport> findOneWithToOneRelationships(@Param("id") UUID id);
}
