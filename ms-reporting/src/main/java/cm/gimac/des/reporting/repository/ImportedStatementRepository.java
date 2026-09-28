package cm.gimac.des.reporting.repository;

import cm.gimac.des.reporting.domain.ImportedStatement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ImportedStatement entity.
 */
@Repository
public interface ImportedStatementRepository extends JpaRepository<ImportedStatement, UUID>, JpaSpecificationExecutor<ImportedStatement> {
    default Optional<ImportedStatement> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<ImportedStatement> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<ImportedStatement> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select importedStatement from ImportedStatement importedStatement left join fetch importedStatement.supersededBy",
        countQuery = "select count(importedStatement) from ImportedStatement importedStatement"
    )
    Page<ImportedStatement> findAllWithToOneRelationships(Pageable pageable);

    @Query("select importedStatement from ImportedStatement importedStatement left join fetch importedStatement.supersededBy")
    List<ImportedStatement> findAllWithToOneRelationships();

    @Query(
        "select importedStatement from ImportedStatement importedStatement left join fetch importedStatement.supersededBy where importedStatement.id =:id"
    )
    Optional<ImportedStatement> findOneWithToOneRelationships(@Param("id") UUID id);
}
