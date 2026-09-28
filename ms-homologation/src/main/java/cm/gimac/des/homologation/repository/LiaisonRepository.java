package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.Liaison;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Liaison entity.
 */
@Repository
public interface LiaisonRepository extends JpaRepository<Liaison, UUID>, JpaSpecificationExecutor<Liaison> {
    default Optional<Liaison> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Liaison> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Liaison> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select liaison from Liaison liaison left join fetch liaison.catalog",
        countQuery = "select count(liaison) from Liaison liaison"
    )
    Page<Liaison> findAllWithToOneRelationships(Pageable pageable);

    @Query("select liaison from Liaison liaison left join fetch liaison.catalog")
    List<Liaison> findAllWithToOneRelationships();

    @Query("select liaison from Liaison liaison left join fetch liaison.catalog where liaison.id =:id")
    Optional<Liaison> findOneWithToOneRelationships(@Param("id") UUID id);
}
