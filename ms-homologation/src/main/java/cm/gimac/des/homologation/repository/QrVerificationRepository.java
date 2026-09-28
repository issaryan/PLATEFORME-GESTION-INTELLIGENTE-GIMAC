package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.QrVerification;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the QrVerification entity.
 */
@Repository
public interface QrVerificationRepository extends JpaRepository<QrVerification, UUID>, JpaSpecificationExecutor<QrVerification> {
    default Optional<QrVerification> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<QrVerification> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<QrVerification> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select qrVerification from QrVerification qrVerification left join fetch qrVerification.liaison left join fetch qrVerification.operation",
        countQuery = "select count(qrVerification) from QrVerification qrVerification"
    )
    Page<QrVerification> findAllWithToOneRelationships(Pageable pageable);

    @Query(
        "select qrVerification from QrVerification qrVerification left join fetch qrVerification.liaison left join fetch qrVerification.operation"
    )
    List<QrVerification> findAllWithToOneRelationships();

    @Query(
        "select qrVerification from QrVerification qrVerification left join fetch qrVerification.liaison left join fetch qrVerification.operation where qrVerification.id =:id"
    )
    Optional<QrVerification> findOneWithToOneRelationships(@Param("id") UUID id);
}
