package cm.gimac.des.certification.repository;

import cm.gimac.des.certification.domain.GoLiveDossier;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the GoLiveDossier entity.
 */
@SuppressWarnings("unused")
@Repository
public interface GoLiveDossierRepository extends JpaRepository<GoLiveDossier, UUID> {}
