package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.sandbox.StatutInstitutionApplique;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Derniers statuts des institutions appliqués aux liaisons (HOM-06, D-122). */
@Repository
public interface StatutInstitutionAppliqueRepository extends JpaRepository<StatutInstitutionApplique, UUID> {
    /** Lecture sous verrou : deux changements de statut d'une même institution s'appliquent l'un après l'autre. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StatutInstitutionApplique s where s.institutionId = :id")
    Optional<StatutInstitutionApplique> verrouiller(@Param("id") UUID institutionId);
}
