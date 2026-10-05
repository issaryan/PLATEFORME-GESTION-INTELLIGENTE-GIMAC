package cm.gimac.des.pilotage.repository;

import cm.gimac.des.pilotage.domain.activite.CategorieEvenement;
import cm.gimac.des.pilotage.domain.activite.EvenementAgenda;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Événements d'agenda (Mon activité, §2.1). */
public interface EvenementAgendaRepository extends JpaRepository<EvenementAgenda, UUID> {
    /** Événements d'un compte (propriétaire ou participant) qui chevauchent [debut, fin[. */
    @EntityGraph(attributePaths = "participants")
    @Query(
        "select e from EvenementAgenda e where (e.proprietaireId = :compte or e.id in " +
        "(select e2.id from EvenementAgenda e2 join e2.participants p where p.participantId = :compte)) " +
        "and e.debut < :fin and e.fin > :debut order by e.debut, e.fin"
    )
    List<EvenementAgenda> agenda(@Param("compte") String compte, @Param("debut") Instant debut, @Param("fin") Instant fin);

    /** Prochains événements d'un compte : ceux qui ne sont pas terminés à l'instant donné. */
    @Query(
        "select e from EvenementAgenda e where (e.proprietaireId = :compte or e.id in " +
        "(select e2.id from EvenementAgenda e2 join e2.participants p where p.participantId = :compte)) " +
        "and e.fin > :apres order by e.debut, e.fin"
    )
    List<EvenementAgenda> prochains(@Param("compte") String compte, @Param("apres") Instant apres, Pageable page);

    /** Propriétaires, parmi les comptes donnés, d'un événement de la catégorie qui chevauche [debut, fin[. */
    @Query(
        "select distinct e.proprietaireId from EvenementAgenda e where e.proprietaireId in :comptes " +
        "and e.categorie = :categorie and e.debut < :fin and e.fin > :debut"
    )
    List<String> proprietairesAvec(
        @Param("comptes") Collection<String> comptes,
        @Param("categorie") CategorieEvenement categorie,
        @Param("debut") Instant debut,
        @Param("fin") Instant fin
    );
}
