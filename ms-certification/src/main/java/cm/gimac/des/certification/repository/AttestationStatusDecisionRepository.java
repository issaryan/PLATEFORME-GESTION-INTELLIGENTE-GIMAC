package cm.gimac.des.certification.repository;

import cm.gimac.des.certification.domain.AttestationStatusDecision;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the AttestationStatusDecision entity.
 */
@SuppressWarnings("unused")
@Repository
public interface AttestationStatusDecisionRepository extends JpaRepository<AttestationStatusDecision, UUID> {}
