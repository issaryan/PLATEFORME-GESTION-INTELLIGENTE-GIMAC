package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.Coverage;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Coverage entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CoverageRepository extends JpaRepository<Coverage, UUID> {}
