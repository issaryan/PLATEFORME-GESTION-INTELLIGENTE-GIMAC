package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.Coverage;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Coverage entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CoverageRepository extends JpaRepository<Coverage, UUID> {
    /** Couvertures reprises du simulateur (HOM-12), par nature, pour une ou plusieurs liaisons. */
    java.util.List<Coverage> findByLiaison_IdInAndNature(java.util.Collection<UUID> liaisonIds, cm.gimac.des.homologation.domain.enumeration.NatureCouverture nature);
}
