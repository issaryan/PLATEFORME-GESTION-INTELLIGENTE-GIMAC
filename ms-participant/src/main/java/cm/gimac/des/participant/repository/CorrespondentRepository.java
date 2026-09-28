package cm.gimac.des.participant.repository;

import cm.gimac.des.participant.domain.Correspondent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Correspondent entity.
 */
@Repository
public interface CorrespondentRepository extends JpaRepository<Correspondent, UUID>, JpaSpecificationExecutor<Correspondent> {
    default Optional<Correspondent> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Correspondent> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Correspondent> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select correspondent from Correspondent correspondent left join fetch correspondent.institution",
        countQuery = "select count(correspondent) from Correspondent correspondent"
    )
    Page<Correspondent> findAllWithToOneRelationships(Pageable pageable);

    @Query("select correspondent from Correspondent correspondent left join fetch correspondent.institution")
    List<Correspondent> findAllWithToOneRelationships();

    @Query("select correspondent from Correspondent correspondent left join fetch correspondent.institution where correspondent.id =:id")
    Optional<Correspondent> findOneWithToOneRelationships(@Param("id") UUID id);
}
