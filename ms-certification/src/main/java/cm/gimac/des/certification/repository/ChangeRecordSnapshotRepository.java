package cm.gimac.des.certification.repository;

import cm.gimac.des.certification.domain.ChangeRecordSnapshot;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ChangeRecordSnapshot entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ChangeRecordSnapshotRepository extends JpaRepository<ChangeRecordSnapshot, UUID> {}
