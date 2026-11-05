package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.PerimetreModification;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Journal des modifications du périmètre d'un projet (D-133). */
@Repository
public interface PerimetreModificationRepository extends JpaRepository<PerimetreModification, UUID> {
    Optional<PerimetreModification> findFirstByLiaisonIdOrderByVersionPerimetreDesc(UUID liaisonId);

    Page<PerimetreModification> findByLiaisonIdOrderByVersionPerimetreDesc(UUID liaisonId, Pageable page);
}
