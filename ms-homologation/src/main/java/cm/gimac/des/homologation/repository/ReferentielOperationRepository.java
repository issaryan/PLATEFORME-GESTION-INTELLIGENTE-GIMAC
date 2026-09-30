package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.parcours.ReferentielOperation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Référentiel de l'annexe A du CR (lecture seule). */
@Repository
public interface ReferentielOperationRepository extends JpaRepository<ReferentielOperation, ReferentielOperation.Cle> {
    List<ReferentielOperation> findByVersionCrOrderByOrdre(String versionCr);
}
