package cm.gimac.des.certification.repository;

import cm.gimac.des.certification.domain.CompletenessCondition;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the CompletenessCondition entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CompletenessConditionRepository extends JpaRepository<CompletenessCondition, UUID> {}
