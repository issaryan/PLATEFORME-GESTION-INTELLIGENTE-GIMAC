package cm.gimac.des.rapprochement.repository;

import cm.gimac.des.rapprochement.domain.AgingRule;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the AgingRule entity.
 */
@SuppressWarnings("unused")
@Repository
public interface AgingRuleRepository extends JpaRepository<AgingRule, UUID> {}
