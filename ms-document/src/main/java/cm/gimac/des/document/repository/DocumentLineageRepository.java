package cm.gimac.des.document.repository;

import cm.gimac.des.document.domain.DocumentLineage;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the DocumentLineage entity.
 */
@SuppressWarnings("unused")
@Repository
public interface DocumentLineageRepository extends JpaRepository<DocumentLineage, UUID>, JpaSpecificationExecutor<DocumentLineage> {}
