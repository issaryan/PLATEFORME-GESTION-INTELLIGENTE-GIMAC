package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.EvidenceReference;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the EvidenceReference entity.
 */
@SuppressWarnings("unused")
@Repository
public interface EvidenceReferenceRepository extends JpaRepository<EvidenceReference, UUID> {}
