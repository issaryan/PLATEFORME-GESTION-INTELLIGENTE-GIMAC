package cm.gimac.des.rapprochement.repository;

import cm.gimac.des.rapprochement.domain.AgingNotice;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the AgingNotice entity.
 */
@Repository
public interface AgingNoticeRepository extends JpaRepository<AgingNotice, UUID> {
    default Optional<AgingNotice> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<AgingNotice> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<AgingNotice> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select agingNotice from AgingNotice agingNotice left join fetch agingNotice.suspens left join fetch agingNotice.rule",
        countQuery = "select count(agingNotice) from AgingNotice agingNotice"
    )
    Page<AgingNotice> findAllWithToOneRelationships(Pageable pageable);

    @Query("select agingNotice from AgingNotice agingNotice left join fetch agingNotice.suspens left join fetch agingNotice.rule")
    List<AgingNotice> findAllWithToOneRelationships();

    @Query(
        "select agingNotice from AgingNotice agingNotice left join fetch agingNotice.suspens left join fetch agingNotice.rule where agingNotice.id =:id"
    )
    Optional<AgingNotice> findOneWithToOneRelationships(@Param("id") UUID id);
}
