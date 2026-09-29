package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.ReplayCursor;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ReplayCursor entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ReplayCursorRepository extends JpaRepository<ReplayCursor, UUID> {}
