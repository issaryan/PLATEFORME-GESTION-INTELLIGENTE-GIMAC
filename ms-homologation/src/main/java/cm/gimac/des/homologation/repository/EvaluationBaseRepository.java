package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.EvaluationBase;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the EvaluationBase entity.
 */
@SuppressWarnings("unused")
@Repository
public interface EvaluationBaseRepository extends JpaRepository<EvaluationBase, UUID> {}
