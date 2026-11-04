package cm.gimac.des.iam.infrastructure.persistence.sandbox;

import cm.gimac.des.iam.domain.AccessReview;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Campagnes de réexamen périodique des accès (US-EXI-23), hors du dépôt généré AccessReviewRepository (D-48 : les
 * fichiers générés ne sont jamais édités).
 */
public interface RevuesAccesRepository extends JpaRepository<AccessReview, UUID> {
    Optional<AccessReview> findByPeriod(String period);

    List<AccessReview> findAllByOrderByOpenedAtDesc();
}
