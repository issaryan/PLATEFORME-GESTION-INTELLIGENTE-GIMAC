package cm.gimac.des.iam.repository;

import cm.gimac.des.iam.domain.RoleGrant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the RoleGrant entity.
 */
@Repository
public interface RoleGrantRepository extends JpaRepository<RoleGrant, UUID>, JpaSpecificationExecutor<RoleGrant> {
    default Optional<RoleGrant> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<RoleGrant> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<RoleGrant> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select roleGrant from RoleGrant roleGrant left join fetch roleGrant.collaborator left join fetch roleGrant.role",
        countQuery = "select count(roleGrant) from RoleGrant roleGrant"
    )
    Page<RoleGrant> findAllWithToOneRelationships(Pageable pageable);

    @Query("select roleGrant from RoleGrant roleGrant left join fetch roleGrant.collaborator left join fetch roleGrant.role")
    List<RoleGrant> findAllWithToOneRelationships();

    @Query(
        "select roleGrant from RoleGrant roleGrant left join fetch roleGrant.collaborator left join fetch roleGrant.role where roleGrant.id =:id"
    )
    Optional<RoleGrant> findOneWithToOneRelationships(@Param("id") UUID id);
}
