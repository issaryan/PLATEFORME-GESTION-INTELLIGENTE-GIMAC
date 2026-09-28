package cm.gimac.des.participant.repository;

import cm.gimac.des.participant.domain.Interaction;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Interaction entity.
 */
@Repository
public interface InteractionRepository extends JpaRepository<Interaction, UUID>, JpaSpecificationExecutor<Interaction> {
    default Optional<Interaction> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Interaction> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Interaction> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select interaction from Interaction interaction left join fetch interaction.institution",
        countQuery = "select count(interaction) from Interaction interaction"
    )
    Page<Interaction> findAllWithToOneRelationships(Pageable pageable);

    @Query("select interaction from Interaction interaction left join fetch interaction.institution")
    List<Interaction> findAllWithToOneRelationships();

    @Query("select interaction from Interaction interaction left join fetch interaction.institution where interaction.id =:id")
    Optional<Interaction> findOneWithToOneRelationships(@Param("id") UUID id);
}
