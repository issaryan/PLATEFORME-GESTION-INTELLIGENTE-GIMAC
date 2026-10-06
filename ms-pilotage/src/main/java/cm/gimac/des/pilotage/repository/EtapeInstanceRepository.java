package cm.gimac.des.pilotage.repository;

import cm.gimac.des.pilotage.domain.processus.EtapeInstance;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Étapes des instances de processus (D-87). */
public interface EtapeInstanceRepository extends JpaRepository<EtapeInstance, UUID> {
    List<EtapeInstance> findByInstanceIdOrderByRangAsc(UUID instanceId);

    List<EtapeInstance> findByInstanceIdInOrderByRangAsc(Collection<UUID> instanceIds);

    Optional<EtapeInstance> findByInstanceIdAndCode(UUID instanceId, String code);

    Optional<EtapeInstance> findByTacheId(UUID tacheId);

    /** Étapes non terminées où le compte est validateur nommé ou signataire attendu (départ d'une personne). */
    @Query(
        value = "select e.* from processus_etape_instance e where e.etat in ('EN_ATTENTE', 'ACTIVE', 'A_SIGNER', 'A_VALIDER') and (e.validateur_id = :compte or " +
        "jsonb_path_exists(e.signatures, '$[*] ? (@.etat == \"ATTENDUE\" && @.attendu.personneId == $m)', jsonb_build_object('m', cast(:compte as text))))",
        nativeQuery = true
    )
    List<EtapeInstance> attenduesDe(@Param("compte") String compte);
}
