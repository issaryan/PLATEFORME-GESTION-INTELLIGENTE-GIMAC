package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.ReferentialVersion;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ReferentialVersion entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ReferentialVersionRepository extends JpaRepository<ReferentialVersion, UUID> {}
