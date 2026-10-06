package cm.gimac.des.pilotage.repository;

import cm.gimac.des.pilotage.domain.activite.EtatTache;
import cm.gimac.des.pilotage.domain.activite.Tache;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Tâches (Mon activité, §2.3). */
public interface TacheRepository extends JpaRepository<Tache, UUID> {
    String TRI = " order by case when t.echeance is null then 1 else 0 end, t.echeance, t.creeLe desc";

    @Query(value = "select t from Tache t where t.responsableId in :responsables and (:etat is null or t.etat = :etat)" + TRI,
        countQuery = "select count(t) from Tache t where t.responsableId in :responsables and (:etat is null or t.etat = :etat)")
    Page<Tache> deResponsables(@Param("responsables") Collection<String> responsables, @Param("etat") EtatTache etat, Pageable page);

    /** Tâches confiées à d'autres, à plusieurs ou à une unité (y compris à prendre, sans responsable). */
    @Query(value = "select t from Tache t where t.createurId = :createur and (t.responsableId is null or t.responsableId <> :createur) and (:etat is null or t.etat = :etat)" + TRI,
        countQuery = "select count(t) from Tache t where t.createurId = :createur and (t.responsableId is null or t.responsableId <> :createur) and (:etat is null or t.etat = :etat)")
    Page<Tache> confiees(@Param("createur") String createur, @Param("etat") EtatTache etat, Pageable page);

    /**
     * Tâches confiées aux unités données, dans l'état donné (vue unite : à prendre, D-81 §6), sans profil exigé ou
     * réservées à l'un des profils donnés (étapes de processus, D-87).
     */
    @Query(value = "select t from Tache t where t.uniteCible in :unites and t.etat = :etat and (t.profilRequis is null or t.profilRequis in :profils)" + TRI,
        countQuery = "select count(t) from Tache t where t.uniteCible in :unites and t.etat = :etat and (t.profilRequis is null or t.profilRequis in :profils)")
    Page<Tache> deUnites(@Param("unites") Collection<String> unites, @Param("etat") EtatTache etat, @Param("profils") Collection<String> profils, Pageable page);

    /** Tâches d'étapes de processus encore ouvertes dont le compte est responsable (départ d'une personne, D-87). */
    @Query("select t from Tache t where t.responsableId = :responsable and t.origineType = cm.gimac.des.pilotage.domain.activite.TypeOrigine.PROCESSUS and t.etat in :etats")
    List<Tache> processusOuvertesDe(@Param("responsable") String responsable, @Param("etats") Collection<EtatTache> etats);

    /** Tâches d'un envoi à plusieurs personnes. */
    List<Tache> findByGroupeIdOrderByCreeLeAsc(UUID groupeId);

    /** Tâches nées d'une origine (clôture et réouverture par le service qui les a créées, D-77). */
    List<Tache> findByOrigineReferenceOrderByCreeLe(String origineReference);

    /** Idempotence de la création par un autre service (§2.3) : origine.reference + responsable. */
    Optional<Tache> findFirstByOrigineReferenceAndResponsableId(String origineReference, String responsableId);

    /** [responsableId, nombre] des tâches dans les états donnés. */
    @Query("select t.responsableId, count(t) from Tache t where t.responsableId in :responsables and t.etat in :etats group by t.responsableId")
    List<Object[]> comptesOuvertes(@Param("responsables") Collection<String> responsables, @Param("etats") Collection<EtatTache> etats);

    /** [responsableId, nombre] des tâches ouvertes dont l'échéance est passée. */
    @Query(
        "select t.responsableId, count(t) from Tache t where t.responsableId in :responsables and t.etat in :etats " +
        "and t.echeance < :jour group by t.responsableId"
    )
    List<Object[]> comptesEnRetard(
        @Param("responsables") Collection<String> responsables,
        @Param("etats") Collection<EtatTache> etats,
        @Param("jour") LocalDate jour
    );

    @Query("select count(t) from Tache t where t.responsableId = :responsable and t.etat in :etats and t.echeance >= :du and t.echeance <= :au")
    long echeancesEntre(
        @Param("responsable") String responsable,
        @Param("etats") Collection<EtatTache> etats,
        @Param("du") LocalDate du,
        @Param("au") LocalDate au
    );

    @Query(
        "select t from Tache t where t.responsableId = :responsable and t.etat = cm.gimac.des.pilotage.domain.activite.EtatTache.TERMINEE " +
        "and t.termineeLe >= :debut and t.termineeLe < :fin order by t.termineeLe"
    )
    List<Tache> termineesEntre(@Param("responsable") String responsable, @Param("debut") Instant debut, @Param("fin") Instant fin);
}
