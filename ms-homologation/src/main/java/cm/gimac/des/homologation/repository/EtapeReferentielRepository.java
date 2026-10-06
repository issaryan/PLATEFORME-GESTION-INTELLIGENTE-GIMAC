package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.EtapeReferentiel;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Étapes des versions du référentiel (D-89). */
@Repository
public interface EtapeReferentielRepository extends JpaRepository<EtapeReferentiel, UUID> {}
