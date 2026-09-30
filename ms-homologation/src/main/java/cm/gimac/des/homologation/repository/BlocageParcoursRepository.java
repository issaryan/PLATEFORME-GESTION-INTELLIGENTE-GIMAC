package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.parcours.BlocageParcours;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Blocages datés des liaisons. */
@Repository
public interface BlocageParcoursRepository extends JpaRepository<BlocageParcours, UUID> {
    List<BlocageParcours> findByLiaisonId(UUID liaisonId);

    List<BlocageParcours> findByLiaisonIdIn(Collection<UUID> liaisonIds);

    /** Liaison d'un blocage, lue sans charger le blocage (il l'est ensuite sous le verrou de la liaison). */
    @Query("select b.liaisonId from BlocageParcours b where b.id = :id")
    java.util.Optional<UUID> liaisonDe(@Param("id") UUID id);
}
