package cm.gimac.des.collaboration.repository;

import cm.gimac.des.collaboration.domain.RoomArchive;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the RoomArchive entity.
 */
@Repository
public interface RoomArchiveRepository extends JpaRepository<RoomArchive, UUID>, JpaSpecificationExecutor<RoomArchive> {
    default Optional<RoomArchive> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<RoomArchive> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<RoomArchive> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select roomArchive from RoomArchive roomArchive left join fetch roomArchive.room",
        countQuery = "select count(roomArchive) from RoomArchive roomArchive"
    )
    Page<RoomArchive> findAllWithToOneRelationships(Pageable pageable);

    @Query("select roomArchive from RoomArchive roomArchive left join fetch roomArchive.room")
    List<RoomArchive> findAllWithToOneRelationships();

    @Query("select roomArchive from RoomArchive roomArchive left join fetch roomArchive.room where roomArchive.id =:id")
    Optional<RoomArchive> findOneWithToOneRelationships(@Param("id") UUID id);
}
