package cm.gimac.des.monitoring.infrastructure.persistance;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Paliers d'escalade des alertes (MON-05). */
public interface EscaladesRepository extends JpaRepository<EscaladeAlerte, UUID> {}
