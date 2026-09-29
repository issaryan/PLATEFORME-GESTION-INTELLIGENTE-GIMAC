package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.ReferentialMigration;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ReferentialMigration entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ReferentialMigrationRepository extends JpaRepository<ReferentialMigration, UUID> {}
