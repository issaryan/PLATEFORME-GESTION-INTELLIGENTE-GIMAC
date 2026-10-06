package cm.gimac.des.pilotage.repository;

import cm.gimac.des.pilotage.domain.processus.InstanceProcessus;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Instances des processus d'unité (D-87). */
public interface InstanceProcessusRepository extends JpaRepository<InstanceProcessus, UUID> {
    @Query(
        value = "select i from InstanceProcessus i where i.lanceurId = :lanceur and (:etat is null or i.etat = :etat) order by i.lanceeLe desc",
        countQuery = "select count(i) from InstanceProcessus i where i.lanceurId = :lanceur and (:etat is null or i.etat = :etat)"
    )
    Page<InstanceProcessus> lancees(@Param("lanceur") String lanceur, @Param("etat") cm.gimac.des.pilotage.domain.processus.EtatInstance etat, Pageable page);

    @Query(
        value = "select i from InstanceProcessus i where (:toutes = true or i.unite in :unites) and (:etat is null or i.etat = :etat) order by i.lanceeLe desc",
        countQuery = "select count(i) from InstanceProcessus i where (:toutes = true or i.unite in :unites) and (:etat is null or i.etat = :etat)"
    )
    Page<InstanceProcessus> deUnites(
        @Param("toutes") boolean toutes,
        @Param("unites") Collection<String> unites,
        @Param("etat") cm.gimac.des.pilotage.domain.processus.EtatInstance etat,
        Pageable page
    );

    /**
     * Instances où le compte est ou a été exécutant (responsable de la tâche d'une étape), validateur ou signataire
     * (attendu ou ayant signé).
     */
    @Query(
        value = "select i.* from processus_instance i where (cast(:etat as varchar) is null or i.etat = cast(:etat as varchar)) and exists (" +
        "select 1 from processus_etape_instance e left join tache t on t.id = e.tache_id where e.instance_id = i.id and (" +
        "t.responsable_id = :moi or e.validateur_id = :moi or jsonb_path_exists(e.signatures, '$[*] ? (@.attendu.personneId == $m || @.signePar == $m)', jsonb_build_object('m', cast(:moi as text)))" +
        ")) order by i.lancee_le desc",
        countQuery = "select count(*) from processus_instance i where (cast(:etat as varchar) is null or i.etat = cast(:etat as varchar)) and exists (" +
        "select 1 from processus_etape_instance e left join tache t on t.id = e.tache_id where e.instance_id = i.id and (" +
        "t.responsable_id = :moi or e.validateur_id = :moi or jsonb_path_exists(e.signatures, '$[*] ? (@.attendu.personneId == $m || @.signePar == $m)', jsonb_build_object('m', cast(:moi as text)))" +
        "))",
        nativeQuery = true
    )
    Page<InstanceProcessus> miennes(@Param("moi") String moi, @Param("etat") String etat, Pageable page);

    long countByProcessusId(UUID processusId);
}
