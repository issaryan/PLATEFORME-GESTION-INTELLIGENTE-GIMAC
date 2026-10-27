package cm.gimac.des.certification.repository;

import cm.gimac.des.certification.domain.Quitus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Quitus entity.
 */
@SuppressWarnings("unused")
@Repository
public interface QuitusRepository extends JpaRepository<Quitus, UUID> {
    List<Quitus> findByCircuit_Id(UUID circuitId);
}
