package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.Liaison;
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
    /** Liaisons d'un type de projet (D-91). */
    long countByTypeProjet(TypeProjet typeProjet);

    /**
     * Référence d'origine déjà portée par une liaison, close ou non (D-104, index partiel uk_liaison_reference_origine).
     * Une institution peut conduire plusieurs liaisons actives, y compris du même type : aucune autre unicité.
     */
    boolean existsByReferenceOrigine(String referenceOrigine);

    /** Liaisons d'une institution (portail, cloisonné par le jeton). */
    java.util.List<Liaison> findByInstitutionId(UUID institutionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Liaison l where l.id = :id")
    Optional<Liaison> findByIdForUpdate(@Param("id") UUID id);
}
