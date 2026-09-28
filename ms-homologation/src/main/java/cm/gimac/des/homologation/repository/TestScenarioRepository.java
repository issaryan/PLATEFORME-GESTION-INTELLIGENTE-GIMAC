package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.TestScenario;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the TestScenario entity.
 */
@Repository
public interface TestScenarioRepository extends JpaRepository<TestScenario, UUID>, JpaSpecificationExecutor<TestScenario> {
    default Optional<TestScenario> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<TestScenario> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<TestScenario> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select testScenario from TestScenario testScenario left join fetch testScenario.liaison left join fetch testScenario.operation",
        countQuery = "select count(testScenario) from TestScenario testScenario"
    )
    Page<TestScenario> findAllWithToOneRelationships(Pageable pageable);

    @Query("select testScenario from TestScenario testScenario left join fetch testScenario.liaison left join fetch testScenario.operation")
    List<TestScenario> findAllWithToOneRelationships();

    @Query(
        "select testScenario from TestScenario testScenario left join fetch testScenario.liaison left join fetch testScenario.operation where testScenario.id =:id"
    )
    Optional<TestScenario> findOneWithToOneRelationships(@Param("id") UUID id);
}
