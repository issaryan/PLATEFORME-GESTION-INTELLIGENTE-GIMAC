package cm.gimac.des.incident.repository;

import cm.gimac.des.incident.domain.Transition;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Transition entity.
 */
@Repository
public interface TransitionRepository extends JpaRepository<Transition, UUID>, JpaSpecificationExecutor<Transition> {
    default Optional<Transition> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Transition> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Transition> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select transition from Transition transition left join fetch transition.ticket",
        countQuery = "select count(transition) from Transition transition"
    )
    Page<Transition> findAllWithToOneRelationships(Pageable pageable);

    @Query("select transition from Transition transition left join fetch transition.ticket")
    List<Transition> findAllWithToOneRelationships();

    @Query("select transition from Transition transition left join fetch transition.ticket where transition.id =:id")
    Optional<Transition> findOneWithToOneRelationships(@Param("id") UUID id);
}
