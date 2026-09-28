package cm.gimac.des.iam.repository;

import cm.gimac.des.iam.domain.Delegation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Delegation entity.
 */
@Repository
public interface DelegationRepository extends JpaRepository<Delegation, UUID>, JpaSpecificationExecutor<Delegation> {
    default Optional<Delegation> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Delegation> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Delegation> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select delegation from Delegation delegation left join fetch delegation.collaborator",
        countQuery = "select count(delegation) from Delegation delegation"
    )
    Page<Delegation> findAllWithToOneRelationships(Pageable pageable);

    @Query("select delegation from Delegation delegation left join fetch delegation.collaborator")
    List<Delegation> findAllWithToOneRelationships();

    @Query("select delegation from Delegation delegation left join fetch delegation.collaborator where delegation.id =:id")
    Optional<Delegation> findOneWithToOneRelationships(@Param("id") UUID id);
}
