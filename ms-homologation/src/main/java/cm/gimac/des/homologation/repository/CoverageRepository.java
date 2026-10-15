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
    /** Mesure d'une liaison pour un code (service, ou mesure globale des essais, D-110). */
    java.util.Optional<Coverage> findFirstByLiaison_IdAndNatureAndServiceCode(UUID liaisonId, cm.gimac.des.homologation.domain.enumeration.NatureCouverture nature, String serviceCode);

    java.util.List<Coverage> findByLiaison_IdInAndNature(java.util.Collection<UUID> liaisonIds, cm.gimac.des.homologation.domain.enumeration.NatureCouverture nature);
}
