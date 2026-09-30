package cm.gimac.des.certification.repository;

import cm.gimac.des.certification.domain.decision.ApprobationSecurite;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Approbations de la sécurité (verrou 2), une par liaison, avec attestations et visas. */
@Repository
public interface ApprobationSecuriteRepository extends JpaRepository<ApprobationSecurite, UUID> {
    @EntityGraph(attributePaths = { "attestations" })
    Optional<ApprobationSecurite> findByLiaisonId(UUID liaisonId);
}
