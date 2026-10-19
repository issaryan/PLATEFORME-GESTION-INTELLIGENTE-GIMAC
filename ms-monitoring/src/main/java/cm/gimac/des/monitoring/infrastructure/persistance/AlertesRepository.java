package cm.gimac.des.monitoring.infrastructure.persistance;

import cm.gimac.des.monitoring.domain.Alert;
import cm.gimac.des.monitoring.domain.enumeration.AlertSeverity;
import cm.gimac.des.monitoring.domain.enumeration.AlertState;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Requêtes de la supervision sur les alertes (algorithme 6.44), à côté du dépôt généré. */
public interface AlertesRepository extends JpaRepository<Alert, UUID> {
    @Query(
        value = "select a from Alert a join fetch a.link left join fetch a.correlationRoot " +
        "where (:etat is null or a.state = :etat) and (:gravite is null or a.severity = :gravite) and (:liaison is null or a.link.id = :liaison) " +
        "order by case when a.state = cm.gimac.des.monitoring.domain.enumeration.AlertState.OUVERTE then 0 " +
        "when a.state = cm.gimac.des.monitoring.domain.enumeration.AlertState.ACQUITTEE then 1 else 2 end, " +
        "a.ackDeadline asc nulls last, a.lastSeenAt desc, a.id",
        countQuery = "select count(a) from Alert a where (:etat is null or a.state = :etat) and (:gravite is null or a.severity = :gravite) " +
        "and (:liaison is null or a.link.id = :liaison)"
    )
    Page<Alert> rechercher(
        @Param("etat") AlertState etat,
        @Param("gravite") AlertSeverity gravite,
        @Param("liaison") UUID liaison,
        Pageable page
    );

    @Query("select a from Alert a join fetch a.link left join fetch a.correlationRoot where a.id = :id")
    Optional<Alert> lire(@Param("id") UUID id);

    /** Alerte non résolue de même empreinte vue depuis {@code depuis} (fenêtre de déduplication). */
    @Query(
        "select a from Alert a join fetch a.link where a.fingerprint = :empreinte and a.state <> cm.gimac.des.monitoring.domain.enumeration.AlertState.RESOLUE " +
        "and a.lastSeenAt >= :depuis order by a.lastSeenAt desc"
    )
    List<Alert> ouvertesParEmpreinte(@Param("empreinte") String empreinte, @Param("depuis") Instant depuis);

    /** Alerte racine non résolue d'une liaison, levée depuis {@code depuis} (fenêtre de corrélation). */
    @Query(
        "select a from Alert a where a.link.id = :liaison and a.correlationRoot is null " +
        "and a.state <> cm.gimac.des.monitoring.domain.enumeration.AlertState.RESOLUE and a.firstSeenAt >= :depuis order by a.firstSeenAt desc"
    )
    List<Alert> racinesOuvertes(@Param("liaison") UUID liaison, @Param("depuis") Instant depuis);

    List<Alert> findByCorrelationRootId(UUID racine);

    @Query("select count(a) from Alert a where a.correlationRoot.id = :racine")
    long nombreDeSymptomes(@Param("racine") UUID racine);

    /** Racines ouvertes dont l'échéance est passée (escalade). */
    @Query(
        "select a from Alert a join fetch a.link where a.state = cm.gimac.des.monitoring.domain.enumeration.AlertState.OUVERTE " +
        "and a.correlationRoot is null and a.ackDeadline is not null and a.ackDeadline <= :maintenant"
    )
    List<Alert> racinesEchues(@Param("maintenant") Instant maintenant);

    /** Nombre d'alertes non résolues par liaison. */
    @Query(
        "select a.link.id, count(a) from Alert a where a.state <> cm.gimac.des.monitoring.domain.enumeration.AlertState.RESOLUE group by a.link.id"
    )
    List<Object[]> ouvertesParLiaison();

    /** Alertes actives sur une période (rapport de quart) : levées avant la fin, non résolues avant le début. */
    @Query("select a from Alert a join fetch a.link where a.firstSeenAt < :fin and (a.resolvedAt is null or a.resolvedAt >= :debut)")
    List<Alert> activesSur(@Param("debut") Instant debut, @Param("fin") Instant fin);

    /** Alertes d'une source, d'une liaison et d'une règle non résolues (résolution par la source). */
    @Query(
        "select a from Alert a where a.fingerprint = :empreinte and a.state <> cm.gimac.des.monitoring.domain.enumeration.AlertState.RESOLUE"
    )
    List<Alert> nonResoluesParEmpreinte(@Param("empreinte") String empreinte);

    long countByReferenceStartingWith(String prefixe);
}
