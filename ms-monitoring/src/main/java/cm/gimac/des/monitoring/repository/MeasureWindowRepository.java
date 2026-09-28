package cm.gimac.des.monitoring.repository;

import cm.gimac.des.monitoring.domain.MeasureWindow;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the MeasureWindow entity.
 */
@Repository
public interface MeasureWindowRepository extends JpaRepository<MeasureWindow, UUID>, JpaSpecificationExecutor<MeasureWindow> {
    default Optional<MeasureWindow> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<MeasureWindow> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<MeasureWindow> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select measureWindow from MeasureWindow measureWindow left join fetch measureWindow.link",
        countQuery = "select count(measureWindow) from MeasureWindow measureWindow"
    )
    Page<MeasureWindow> findAllWithToOneRelationships(Pageable pageable);

    @Query("select measureWindow from MeasureWindow measureWindow left join fetch measureWindow.link")
    List<MeasureWindow> findAllWithToOneRelationships();

    @Query("select measureWindow from MeasureWindow measureWindow left join fetch measureWindow.link where measureWindow.id =:id")
    Optional<MeasureWindow> findOneWithToOneRelationships(@Param("id") UUID id);
}
