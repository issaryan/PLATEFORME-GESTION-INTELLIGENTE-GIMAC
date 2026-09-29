package cm.gimac.des.iam.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CleSecuriteRepository extends JpaRepository<CleSecurite, UUID> {
    List<CleSecurite> findBySujetOrderByCreeeLe(String sujet);

    Optional<CleSecurite> findByIdentifiantCle(String identifiantCle);

    long countBySujet(String sujet);
}
