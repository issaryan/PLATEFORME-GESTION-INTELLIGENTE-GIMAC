package cm.gimac.des.certification.repository;

import cm.gimac.des.certification.domain.CertificationCircuit;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the CertificationCircuit entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CertificationCircuitRepository extends JpaRepository<CertificationCircuit, UUID> {
    Optional<CertificationCircuit> findFirstByLiaisonIdOrderByCycleDesc(UUID liaisonId);
}
