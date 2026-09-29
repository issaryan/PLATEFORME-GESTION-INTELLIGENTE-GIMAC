package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.UnmatchedScenario;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the UnmatchedScenario entity.
 */
@SuppressWarnings("unused")
@Repository
public interface UnmatchedScenarioRepository extends JpaRepository<UnmatchedScenario, UUID> {}
