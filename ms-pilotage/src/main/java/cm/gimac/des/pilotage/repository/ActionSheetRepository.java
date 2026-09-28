package cm.gimac.des.pilotage.repository;

import cm.gimac.des.pilotage.domain.ActionSheet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ActionSheet entity.
 */
@Repository
public interface ActionSheetRepository extends JpaRepository<ActionSheet, UUID>, JpaSpecificationExecutor<ActionSheet> {
    default Optional<ActionSheet> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<ActionSheet> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<ActionSheet> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select actionSheet from ActionSheet actionSheet left join fetch actionSheet.risk",
        countQuery = "select count(actionSheet) from ActionSheet actionSheet"
    )
    Page<ActionSheet> findAllWithToOneRelationships(Pageable pageable);

    @Query("select actionSheet from ActionSheet actionSheet left join fetch actionSheet.risk")
    List<ActionSheet> findAllWithToOneRelationships();

    @Query("select actionSheet from ActionSheet actionSheet left join fetch actionSheet.risk where actionSheet.id =:id")
    Optional<ActionSheet> findOneWithToOneRelationships(@Param("id") UUID id);
}
