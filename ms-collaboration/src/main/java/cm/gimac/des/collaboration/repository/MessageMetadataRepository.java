package cm.gimac.des.collaboration.repository;

import cm.gimac.des.collaboration.domain.MessageMetadata;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the MessageMetadata entity.
 */
@Repository
public interface MessageMetadataRepository extends JpaRepository<MessageMetadata, UUID>, JpaSpecificationExecutor<MessageMetadata> {
    default Optional<MessageMetadata> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<MessageMetadata> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<MessageMetadata> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select messageMetadata from MessageMetadata messageMetadata left join fetch messageMetadata.room",
        countQuery = "select count(messageMetadata) from MessageMetadata messageMetadata"
    )
    Page<MessageMetadata> findAllWithToOneRelationships(Pageable pageable);

    @Query("select messageMetadata from MessageMetadata messageMetadata left join fetch messageMetadata.room")
    List<MessageMetadata> findAllWithToOneRelationships();

    @Query("select messageMetadata from MessageMetadata messageMetadata left join fetch messageMetadata.room where messageMetadata.id =:id")
    Optional<MessageMetadata> findOneWithToOneRelationships(@Param("id") UUID id);
}
