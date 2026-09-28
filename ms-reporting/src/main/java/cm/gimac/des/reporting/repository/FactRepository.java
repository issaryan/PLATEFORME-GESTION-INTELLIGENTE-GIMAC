package cm.gimac.des.reporting.repository;

import cm.gimac.des.reporting.domain.Fact;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Fact entity.
 */
@Repository
public interface FactRepository extends JpaRepository<Fact, UUID>, JpaSpecificationExecutor<Fact> {
    default Optional<Fact> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Fact> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Fact> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(value = "select fact from Fact fact left join fetch fact.statement", countQuery = "select count(fact) from Fact fact")
    Page<Fact> findAllWithToOneRelationships(Pageable pageable);

    @Query("select fact from Fact fact left join fetch fact.statement")
    List<Fact> findAllWithToOneRelationships();

    @Query("select fact from Fact fact left join fetch fact.statement where fact.id =:id")
    Optional<Fact> findOneWithToOneRelationships(@Param("id") UUID id);
}
