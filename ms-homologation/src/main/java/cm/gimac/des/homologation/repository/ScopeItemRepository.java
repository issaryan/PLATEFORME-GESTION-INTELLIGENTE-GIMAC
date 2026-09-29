package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.ScopeItem;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ScopeItem entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ScopeItemRepository extends JpaRepository<ScopeItem, UUID> {}
