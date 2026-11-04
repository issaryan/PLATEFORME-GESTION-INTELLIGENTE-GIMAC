package cm.gimac.des.certification.infrastructure.persistence;

import cm.gimac.des.certification.domain.CertificationCircuit;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Circuit de certification courant d'une liaison (US-EXI-25), hors du dépôt généré CertificationCircuitRepository (D-48). */
public interface CircuitsLiaisonRepository extends JpaRepository<CertificationCircuit, UUID> {
    Optional<CertificationCircuit> findFirstByLiaisonIdOrderByCycleDesc(UUID liaisonId);
}
