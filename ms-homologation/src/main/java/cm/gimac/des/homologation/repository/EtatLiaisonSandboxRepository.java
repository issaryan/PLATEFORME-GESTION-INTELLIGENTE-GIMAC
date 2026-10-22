package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.sandbox.EtatLiaisonSandbox;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Derniers états publiés des liaisons pour la Sandbox GIMAC (US-EXI-16). */
@Repository
public interface EtatLiaisonSandboxRepository extends JpaRepository<EtatLiaisonSandbox, UUID> {
    /** Lecture sous verrou : deux transactions concurrentes ne publient jamais la même version. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EtatLiaisonSandbox e where e.liaisonId = :id")
    Optional<EtatLiaisonSandbox> verrouiller(@Param("id") UUID liaisonId);
}
