package cm.gimac.des.iam.repository;

import cm.gimac.des.iam.domain.UserSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the UserSession entity.
 */
@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID>, JpaSpecificationExecutor<UserSession> {
    default Optional<UserSession> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<UserSession> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<UserSession> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select userSession from UserSession userSession left join fetch userSession.collaborator",
        countQuery = "select count(userSession) from UserSession userSession"
    )
    Page<UserSession> findAllWithToOneRelationships(Pageable pageable);

    @Query("select userSession from UserSession userSession left join fetch userSession.collaborator")
    List<UserSession> findAllWithToOneRelationships();

    @Query("select userSession from UserSession userSession left join fetch userSession.collaborator where userSession.id =:id")
    Optional<UserSession> findOneWithToOneRelationships(@Param("id") UUID id);
}
