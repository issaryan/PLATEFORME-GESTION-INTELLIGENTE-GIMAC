package cm.gimac.des.pilotage.repository;

import cm.gimac.des.pilotage.domain.WipOverride;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the WipOverride entity.
 */
@Repository
public interface WipOverrideRepository extends JpaRepository<WipOverride, UUID>, JpaSpecificationExecutor<WipOverride> {
    default Optional<WipOverride> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<WipOverride> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<WipOverride> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select wipOverride from WipOverride wipOverride left join fetch wipOverride.boardColumn left join fetch wipOverride.task",
        countQuery = "select count(wipOverride) from WipOverride wipOverride"
    )
    Page<WipOverride> findAllWithToOneRelationships(Pageable pageable);

    @Query("select wipOverride from WipOverride wipOverride left join fetch wipOverride.boardColumn left join fetch wipOverride.task")
    List<WipOverride> findAllWithToOneRelationships();

    @Query(
        "select wipOverride from WipOverride wipOverride left join fetch wipOverride.boardColumn left join fetch wipOverride.task where wipOverride.id =:id"
    )
    Optional<WipOverride> findOneWithToOneRelationships(@Param("id") UUID id);
}
