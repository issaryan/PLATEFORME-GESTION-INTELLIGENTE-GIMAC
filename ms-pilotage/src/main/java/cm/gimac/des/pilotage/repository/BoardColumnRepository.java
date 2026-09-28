package cm.gimac.des.pilotage.repository;

import cm.gimac.des.pilotage.domain.BoardColumn;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the BoardColumn entity.
 */
@Repository
public interface BoardColumnRepository extends JpaRepository<BoardColumn, UUID> {
    default Optional<BoardColumn> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<BoardColumn> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<BoardColumn> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select boardColumn from BoardColumn boardColumn left join fetch boardColumn.board",
        countQuery = "select count(boardColumn) from BoardColumn boardColumn"
    )
    Page<BoardColumn> findAllWithToOneRelationships(Pageable pageable);

    @Query("select boardColumn from BoardColumn boardColumn left join fetch boardColumn.board")
    List<BoardColumn> findAllWithToOneRelationships();

    @Query("select boardColumn from BoardColumn boardColumn left join fetch boardColumn.board where boardColumn.id =:id")
    Optional<BoardColumn> findOneWithToOneRelationships(@Param("id") UUID id);
}
