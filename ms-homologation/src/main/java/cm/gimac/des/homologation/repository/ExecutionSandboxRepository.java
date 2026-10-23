package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.sandbox.ExecutionSandbox;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Exécutions de la sandbox reprises, une par campagne et exécution (US-EXI-14). */
@Repository
public interface ExecutionSandboxRepository extends JpaRepository<ExecutionSandbox, UUID> {
    Optional<ExecutionSandbox> findByCampagneIdAndIdentifiantExecution(String campagneId, String identifiantExecution);

    Page<ExecutionSandbox> findByLiaisonIdAndCampagneId(UUID liaisonId, String campagneId, Pageable page);

    long countByCampagneId(String campagneId);

    long countByCampagneIdAndOperationCodeIsNull(String campagneId);
}
