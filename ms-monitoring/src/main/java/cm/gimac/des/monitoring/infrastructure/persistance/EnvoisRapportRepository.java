package cm.gimac.des.monitoring.infrastructure.persistance;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Envois des rapports (D-116 §10.1). */
public interface EnvoisRapportRepository extends JpaRepository<EnvoiRapport, UUID> {
    List<EnvoiRapport> findByRapportIdOrderByLeAsc(UUID rapport);

    boolean existsByRapportIdAndDestinataireId(UUID rapport, UUID destinataire);
}
