package cm.gimac.des.iam.repository;

import cm.gimac.des.iam.domain.Availability;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Availability entity.
 */
@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, UUID>, JpaSpecificationExecutor<Availability> {
    default Optional<Availability> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Availability> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Availability> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select availability from Availability availability left join fetch availability.collaborator",
        countQuery = "select count(availability) from Availability availability"
    )
    Page<Availability> findAllWithToOneRelationships(Pageable pageable);

    @Query("select availability from Availability availability left join fetch availability.collaborator")
    List<Availability> findAllWithToOneRelationships();

    @Query("select availability from Availability availability left join fetch availability.collaborator where availability.id =:id")
    Optional<Availability> findOneWithToOneRelationships(@Param("id") UUID id);
}
