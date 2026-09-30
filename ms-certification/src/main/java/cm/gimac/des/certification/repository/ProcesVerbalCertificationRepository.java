package cm.gimac.des.certification.repository;

import cm.gimac.des.certification.domain.decision.ProcesVerbalCertification;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Procès-verbaux de certification (verrou 1), un par liaison. */
@Repository
public interface ProcesVerbalCertificationRepository extends JpaRepository<ProcesVerbalCertification, UUID> {
    Optional<ProcesVerbalCertification> findByLiaisonId(UUID liaisonId);
}
