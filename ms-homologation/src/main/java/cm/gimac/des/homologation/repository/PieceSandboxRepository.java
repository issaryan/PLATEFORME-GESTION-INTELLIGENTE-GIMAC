package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.sandbox.PieceSandbox;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Pièces des campagnes de la sandbox reprises (US-EXI-14). */
@Repository
public interface PieceSandboxRepository extends JpaRepository<PieceSandbox, UUID> {
    boolean existsByCampagneIdAndNatureAndVersion(String campagneId, String nature, int version);

    Optional<PieceSandbox> findByCampagneIdAndNatureAndVersion(String campagneId, String nature, int version);

    List<PieceSandbox> findByCampagneIdOrderByNatureAscVersionAsc(String campagneId);

    /** Pièce déjà conservée de même empreinte (déposée par cette relève ou reconnue). */
    Optional<PieceSandbox> findFirstByEmpreinteAndDocumentIdIsNotNull(String empreinte);
}
