package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.ReferenceCatalog;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ReferenceCatalog entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ReferenceCatalogRepository extends JpaRepository<ReferenceCatalog, UUID> {}
