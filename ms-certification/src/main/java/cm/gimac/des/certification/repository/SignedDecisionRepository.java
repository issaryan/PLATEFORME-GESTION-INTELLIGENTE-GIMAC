package cm.gimac.des.certification.repository;

import cm.gimac.des.certification.domain.SignedDecision;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the SignedDecision entity.
 */
@SuppressWarnings("unused")
@Repository
public interface SignedDecisionRepository extends JpaRepository<SignedDecision, UUID> {}
