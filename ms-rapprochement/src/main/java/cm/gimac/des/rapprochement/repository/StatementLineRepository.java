package cm.gimac.des.rapprochement.repository;

import cm.gimac.des.rapprochement.domain.StatementLine;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the StatementLine entity.
 */
@Repository
public interface StatementLineRepository extends JpaRepository<StatementLine, UUID>, JpaSpecificationExecutor<StatementLine> {
    default Optional<StatementLine> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<StatementLine> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<StatementLine> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select statementLine from StatementLine statementLine left join fetch statementLine.statement",
        countQuery = "select count(statementLine) from StatementLine statementLine"
    )
    Page<StatementLine> findAllWithToOneRelationships(Pageable pageable);

    @Query("select statementLine from StatementLine statementLine left join fetch statementLine.statement")
    List<StatementLine> findAllWithToOneRelationships();

    @Query("select statementLine from StatementLine statementLine left join fetch statementLine.statement where statementLine.id =:id")
    Optional<StatementLine> findOneWithToOneRelationships(@Param("id") UUID id);
}
