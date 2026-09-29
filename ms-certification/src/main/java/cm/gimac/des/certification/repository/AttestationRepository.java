package cm.gimac.des.certification.repository;

import cm.gimac.des.certification.domain.Attestation;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Attestation entity.
 */
@SuppressWarnings("unused")
@Repository
public interface AttestationRepository extends JpaRepository<Attestation, UUID> {}
