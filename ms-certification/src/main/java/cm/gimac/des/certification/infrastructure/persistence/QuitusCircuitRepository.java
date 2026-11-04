package cm.gimac.des.certification.infrastructure.persistence;

import cm.gimac.des.certification.domain.Quitus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Quitus d'un circuit de certification (caducité, US-EXI-25), hors du dépôt généré QuitusRepository (D-48). */
public interface QuitusCircuitRepository extends JpaRepository<Quitus, UUID> {
    List<Quitus> findByCircuit_Id(UUID circuitId);
}
