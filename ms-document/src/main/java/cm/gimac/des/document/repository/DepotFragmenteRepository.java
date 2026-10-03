package cm.gimac.des.document.repository;

import cm.gimac.des.document.domain.DepotFragmente;
import cm.gimac.des.document.domain.enumeration.EtatDepotFragmente;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Dépôts fragmentés en cours ou terminés (D-74). */
@Repository
public interface DepotFragmenteRepository extends JpaRepository<DepotFragmente, UUID> {
    /** Dépôt du compte {@code deposant} : un dépôt d'un autre compte est traité comme inconnu. */
    Optional<DepotFragmente> findByIdAndDeposant(UUID id, String deposant);

    /**
     * Même lecture, ligne verrouillée jusqu'à la fin de la transaction : réception d'une partie,
     * fin et abandon d'un même dépôt s'exécutent l'un après l'autre.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DepotFragmente d where d.id = :id and d.deposant = :deposant")
    Optional<DepotFragmente> verrouiller(@Param("id") UUID id, @Param("deposant") String deposant);

    /** Ligne verrouillée, sans contrôle du déposant (tâches de fond). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DepotFragmente d where d.id = :id")
    Optional<DepotFragmente> verrouiller(@Param("id") UUID id);

    /** Dépôts à vérifier : assemblés, dont l'instant de reprise est atteint (ou non fixé), les plus anciens d'abord. */
    @Query(
        "select d.id from DepotFragmente d where d.etat = :etat and (d.repriseLe is null or d.repriseLe <= :maintenant) order by d.actifLe"
    )
    List<UUID> aVerifier(@Param("etat") EtatDepotFragmente etat, @Param("maintenant") Instant maintenant, Pageable page);

    /** Dépôts dans l'état {@code etat} sans activité depuis {@code seuil}. */
    @Query("select d.id from DepotFragmente d where d.etat = :etat and d.actifLe < :seuil order by d.actifLe")
    List<UUID> inactifs(@Param("etat") EtatDepotFragmente etat, @Param("seuil") Instant seuil, Pageable page);
}
