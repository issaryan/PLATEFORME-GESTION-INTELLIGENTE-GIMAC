package cm.gimac.des.rapprochement.repository;

import cm.gimac.des.rapprochement.domain.ImportedStatement;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ImportedStatement entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ImportedStatementRepository extends JpaRepository<ImportedStatement, UUID>, JpaSpecificationExecutor<ImportedStatement> {}
