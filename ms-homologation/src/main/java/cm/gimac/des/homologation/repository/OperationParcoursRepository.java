package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.parcours.OperationParcours;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Opérations instanciées du parcours. */
@Repository
public interface OperationParcoursRepository extends JpaRepository<OperationParcours, UUID> {
    List<OperationParcours> findByLiaisonIdOrderByOrdre(UUID liaisonId);

    List<OperationParcours> findByLiaisonIdIn(Collection<UUID> liaisonIds);

    /** Liaison d'une opération, lue sans charger l'opération (elle l'est ensuite sous le verrou de la liaison). */
    @Query("select o.liaisonId from OperationParcours o where o.id = :id")
    java.util.Optional<UUID> liaisonDe(@Param("id") UUID id);

    /**
     * Liaisons non closes où un compte tient une qualité sur une opération non faite, une ressource ou la conduite
     * (départ d'un compte, D-93 §1) ; {@code motif} : l'identifiant du compte entre guillemets.
     */
    @Query(
        value = "select distinct l.id from liaison l left join parcours_liaison pl on pl.liaison_id = l.id " +
        "where l.etat <> 'CLOSE' and (cast(l.chef_de_projet_id as varchar) = :compte or cast(pl.ressources as text) like :motif " +
        "or exists (select 1 from parcours_operation o where o.liaison_id = l.id and o.etat not in ('FAITE', 'SANS_OBJET') and cast(o.raci as text) like :motif))",
        nativeQuery = true
    )
    List<UUID> liaisonsImpliquant(@Param("compte") String compte, @Param("motif") String motif);
}
