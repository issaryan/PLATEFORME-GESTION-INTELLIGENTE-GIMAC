package cm.gimac.des.monitoring.infrastructure.persistance;

import cm.gimac.des.monitoring.domain.AlertSeverityPolicy;
import cm.gimac.des.monitoring.domain.enumeration.AlertSeverity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Politiques d'alerte par gravité (tableau 6.48). */
public interface PolitiquesRepository extends JpaRepository<AlertSeverityPolicy, UUID> {
    Optional<AlertSeverityPolicy> findBySeverity(AlertSeverity gravite);
}
