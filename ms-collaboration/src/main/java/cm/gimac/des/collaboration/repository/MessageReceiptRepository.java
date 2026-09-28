package cm.gimac.des.collaboration.repository;

import cm.gimac.des.collaboration.domain.MessageReceipt;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the MessageReceipt entity.
 */
@Repository
public interface MessageReceiptRepository extends JpaRepository<MessageReceipt, UUID> {
    default Optional<MessageReceipt> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<MessageReceipt> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<MessageReceipt> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select messageReceipt from MessageReceipt messageReceipt left join fetch messageReceipt.message",
        countQuery = "select count(messageReceipt) from MessageReceipt messageReceipt"
    )
    Page<MessageReceipt> findAllWithToOneRelationships(Pageable pageable);

    @Query("select messageReceipt from MessageReceipt messageReceipt left join fetch messageReceipt.message")
    List<MessageReceipt> findAllWithToOneRelationships();

    @Query("select messageReceipt from MessageReceipt messageReceipt left join fetch messageReceipt.message where messageReceipt.id =:id")
    Optional<MessageReceipt> findOneWithToOneRelationships(@Param("id") UUID id);
}
