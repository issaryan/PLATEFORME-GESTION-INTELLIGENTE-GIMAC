package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Liaison entity.
 */
@SuppressWarnings("unused")
@Repository
public interface LiaisonRepository extends JpaRepository<Liaison, UUID>, JpaSpecificationExecutor<Liaison> {
    /** Unicité des liaisons actives par institution et type de projet (index partiel uk_liaison_active). */
    boolean existsByInstitutionIdAndTypeProjetAndEtatNot(UUID institutionId, TypeProjet typeProjet, EtatLiaison etat);

    /** Liaison principale active (un service supplémentaire, rang 35, est sans effet sur l'unicité). */
    boolean existsByInstitutionIdAndTypeProjetAndEtatNotAndLiaisonOrigineIdIsNull(UUID institutionId, TypeProjet typeProjet, EtatLiaison etat);

    /** Liaisons d'une institution (portail, cloisonné par le jeton). */
    java.util.List<Liaison> findByInstitutionId(UUID institutionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Liaison l where l.id = :id")
    Optional<Liaison> findByIdForUpdate(@Param("id") UUID id);
}
