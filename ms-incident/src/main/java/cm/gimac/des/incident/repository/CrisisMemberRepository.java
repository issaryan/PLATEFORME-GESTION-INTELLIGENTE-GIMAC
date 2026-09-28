package cm.gimac.des.incident.repository;

import cm.gimac.des.incident.domain.CrisisMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the CrisisMember entity.
 */
@Repository
public interface CrisisMemberRepository extends JpaRepository<CrisisMember, UUID> {
    default Optional<CrisisMember> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<CrisisMember> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<CrisisMember> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select crisisMember from CrisisMember crisisMember left join fetch crisisMember.cell",
        countQuery = "select count(crisisMember) from CrisisMember crisisMember"
    )
    Page<CrisisMember> findAllWithToOneRelationships(Pageable pageable);

    @Query("select crisisMember from CrisisMember crisisMember left join fetch crisisMember.cell")
    List<CrisisMember> findAllWithToOneRelationships();

    @Query("select crisisMember from CrisisMember crisisMember left join fetch crisisMember.cell where crisisMember.id =:id")
    Optional<CrisisMember> findOneWithToOneRelationships(@Param("id") UUID id);
}
