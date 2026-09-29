package cm.gimac.des.certification.repository;

import cm.gimac.des.certification.domain.CertificationRegisterEntry;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the CertificationRegisterEntry entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CertificationRegisterEntryRepository extends JpaRepository<CertificationRegisterEntry, UUID> {}
