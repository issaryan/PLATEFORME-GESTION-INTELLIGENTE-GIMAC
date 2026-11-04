package cm.gimac.des.iam.infrastructure.persistence.sandbox;

import cm.gimac.des.iam.domain.AccessReviewDecision;
import cm.gimac.des.iam.domain.enumeration.ReviewDecision;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Décisions d'une campagne de réexamen des accès (US-EXI-23), hors du dépôt généré AccessReviewDecisionRepository
 * (D-48).
 */
public interface DecisionsRevueRepository extends JpaRepository<AccessReviewDecision, UUID> {
    List<AccessReviewDecision> findByReviewIdOrderByCollaboratorFederatedId(UUID reviewId);

    Optional<AccessReviewDecision> findByReviewIdAndCollaboratorFederatedId(UUID reviewId, String federatedId);

    long countByReviewIdAndDecision(UUID reviewId, ReviewDecision decision);
}
