package cm.gimac.des.participant.repository;

import cm.gimac.des.participant.domain.Institution;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Institution entity.
 */
@Repository
public interface InstitutionRepository extends JpaRepository<Institution, UUID>, JpaSpecificationExecutor<Institution> {
    default Optional<Institution> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Institution> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Institution> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select institution from Institution institution left join fetch institution.settlementBank left join fetch institution.sponsorBank",
        countQuery = "select count(institution) from Institution institution"
    )
    Page<Institution> findAllWithToOneRelationships(Pageable pageable);

    @Query(
        "select institution from Institution institution left join fetch institution.settlementBank left join fetch institution.sponsorBank"
    )
    List<Institution> findAllWithToOneRelationships();

    @Query(
        "select institution from Institution institution left join fetch institution.settlementBank left join fetch institution.sponsorBank where institution.id =:id"
    )
    Optional<Institution> findOneWithToOneRelationships(@Param("id") UUID id);

    /** Verrou pessimiste : une seule décision à la fois sur une institution (6.25). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select institution from Institution institution where institution.id = :id")
    Optional<Institution> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByParticipantCodeAndIdNot(String participantCode, UUID id);

    boolean existsByIdentificationCodeAndIdNot(String identificationCode, UUID id);

    boolean existsByBeacBankCodeAndIdNot(String beacBankCode, UUID id);

    long countByStatus(InstitutionStatus status);

    /** Prises de contact (création du dossier) depuis « depuis » : flux mensuel du tableau de bord de la DDRM. */
    @Query("select i.createdAt from Institution i where i.createdAt >= :depuis")
    List<java.time.Instant> creationsDepuis(@Param("depuis") java.time.Instant depuis);

    /** Adhésions prononcées depuis « depuis ». */
    @Query("select i.admittedAt from Institution i where i.admittedAt >= :depuis")
    List<java.time.Instant> adhesionsDepuis(@Param("depuis") java.time.Instant depuis);

    /** Clôtures (sans suite) depuis « depuis ». */
    @Query("select i.closedAt from Institution i where i.closedAt >= :depuis")
    List<java.time.Instant> cloturesDepuis(@Param("depuis") java.time.Instant depuis);
}
