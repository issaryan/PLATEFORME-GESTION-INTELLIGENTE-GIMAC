package cm.gimac.des.reporting.repository;

import cm.gimac.des.reporting.domain.IndicatorValue;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the IndicatorValue entity.
 */
@Repository
public interface IndicatorValueRepository extends JpaRepository<IndicatorValue, UUID>, JpaSpecificationExecutor<IndicatorValue> {
    default Optional<IndicatorValue> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<IndicatorValue> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<IndicatorValue> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select indicatorValue from IndicatorValue indicatorValue left join fetch indicatorValue.definition left join fetch indicatorValue.statement",
        countQuery = "select count(indicatorValue) from IndicatorValue indicatorValue"
    )
    Page<IndicatorValue> findAllWithToOneRelationships(Pageable pageable);

    @Query(
        "select indicatorValue from IndicatorValue indicatorValue left join fetch indicatorValue.definition left join fetch indicatorValue.statement"
    )
    List<IndicatorValue> findAllWithToOneRelationships();

    @Query(
        "select indicatorValue from IndicatorValue indicatorValue left join fetch indicatorValue.definition left join fetch indicatorValue.statement where indicatorValue.id =:id"
    )
    Optional<IndicatorValue> findOneWithToOneRelationships(@Param("id") UUID id);
}
