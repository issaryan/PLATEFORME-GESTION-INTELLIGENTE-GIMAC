package cm.gimac.des.certification.repository;

import cm.gimac.des.certification.domain.Quitus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Quitus entity.
 */
@Repository
public interface QuitusRepository extends JpaRepository<Quitus, UUID>, JpaSpecificationExecutor<Quitus> {
    default Optional<Quitus> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Quitus> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Quitus> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(value = "select quitus from Quitus quitus left join fetch quitus.file", countQuery = "select count(quitus) from Quitus quitus")
    Page<Quitus> findAllWithToOneRelationships(Pageable pageable);

    @Query("select quitus from Quitus quitus left join fetch quitus.file")
    List<Quitus> findAllWithToOneRelationships();

    @Query("select quitus from Quitus quitus left join fetch quitus.file where quitus.id =:id")
    Optional<Quitus> findOneWithToOneRelationships(@Param("id") UUID id);
}
