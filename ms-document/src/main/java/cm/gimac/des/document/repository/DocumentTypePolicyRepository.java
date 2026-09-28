package cm.gimac.des.document.repository;

import cm.gimac.des.document.domain.DocumentTypePolicy;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the DocumentTypePolicy entity.
 */
@SuppressWarnings("unused")
@Repository
public interface DocumentTypePolicyRepository
    extends JpaRepository<DocumentTypePolicy, UUID>, JpaSpecificationExecutor<DocumentTypePolicy> {}
