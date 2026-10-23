package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.sandbox.CampagneSandbox;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Campagnes de la sandbox reprises (US-EXI-14). */
@Repository
public interface CampagneSandboxRepository extends JpaRepository<CampagneSandbox, String> {
    List<CampagneSandbox> findByLiaisonIdOrderByCampagneId(UUID liaisonId);

    /** Lecture sous verrou : deux relèves concurrentes d'une même campagne se succèdent sur sa position. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CampagneSandbox c where c.campagneId = :id")
    Optional<CampagneSandbox> verrouiller(@Param("id") String campagneId);
}
