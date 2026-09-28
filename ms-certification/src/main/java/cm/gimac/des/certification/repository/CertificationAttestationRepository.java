package cm.gimac.des.certification.repository;

import cm.gimac.des.certification.domain.CertificationAttestation;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the CertificationAttestation entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CertificationAttestationRepository extends JpaRepository<CertificationAttestation, UUID> {}
