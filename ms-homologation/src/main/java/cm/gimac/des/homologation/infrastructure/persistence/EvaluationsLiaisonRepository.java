package cm.gimac.des.homologation.infrastructure.persistence;

import cm.gimac.des.homologation.domain.EvaluationBase;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Dernier socle d'évaluation d'une liaison (US-EXI-25), hors du dépôt généré EvaluationBaseRepository (D-48). */
public interface EvaluationsLiaisonRepository extends JpaRepository<EvaluationBase, UUID> {
    Optional<EvaluationBase> findFirstByLiaison_IdOrderByCycleDesc(UUID liaisonId);
}
