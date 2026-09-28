package cm.gimac.des.certification.repository;

import cm.gimac.des.certification.domain.DeduplicationEntry;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the DeduplicationEntry entity.
 */
@SuppressWarnings("unused")
@Repository
public interface DeduplicationEntryRepository extends JpaRepository<DeduplicationEntry, UUID> {}
