package cm.gimac.des.certification.repository;

import cm.gimac.des.certification.domain.CommissioningFile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the CommissioningFile entity.
 */
@Repository
public interface CommissioningFileRepository extends JpaRepository<CommissioningFile, UUID>, JpaSpecificationExecutor<CommissioningFile> {
    default Optional<CommissioningFile> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<CommissioningFile> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<CommissioningFile> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select commissioningFile from CommissioningFile commissioningFile left join fetch commissioningFile.changeRecord left join fetch commissioningFile.attestation",
        countQuery = "select count(commissioningFile) from CommissioningFile commissioningFile"
    )
    Page<CommissioningFile> findAllWithToOneRelationships(Pageable pageable);

    @Query(
        "select commissioningFile from CommissioningFile commissioningFile left join fetch commissioningFile.changeRecord left join fetch commissioningFile.attestation"
    )
    List<CommissioningFile> findAllWithToOneRelationships();

    @Query(
        "select commissioningFile from CommissioningFile commissioningFile left join fetch commissioningFile.changeRecord left join fetch commissioningFile.attestation where commissioningFile.id =:id"
    )
    Optional<CommissioningFile> findOneWithToOneRelationships(@Param("id") UUID id);
}
