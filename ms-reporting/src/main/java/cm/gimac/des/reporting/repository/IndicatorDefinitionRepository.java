package cm.gimac.des.reporting.repository;

import cm.gimac.des.reporting.domain.IndicatorDefinition;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the IndicatorDefinition entity.
 */
@SuppressWarnings("unused")
@Repository
public interface IndicatorDefinitionRepository
    extends JpaRepository<IndicatorDefinition, UUID>, JpaSpecificationExecutor<IndicatorDefinition> {}
