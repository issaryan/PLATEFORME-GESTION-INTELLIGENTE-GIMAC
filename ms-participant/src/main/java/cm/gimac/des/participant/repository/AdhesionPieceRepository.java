package cm.gimac.des.participant.repository;

import cm.gimac.des.participant.domain.AdhesionPiece;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import cm.gimac.des.participant.domain.enumeration.AdhesionPieceState;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the AdhesionPiece entity.
 */
@Repository
public interface AdhesionPieceRepository extends JpaRepository<AdhesionPiece, UUID> {
    default Optional<AdhesionPiece> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<AdhesionPiece> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<AdhesionPiece> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select adhesionPiece from AdhesionPiece adhesionPiece left join fetch adhesionPiece.institution",
        countQuery = "select count(adhesionPiece) from AdhesionPiece adhesionPiece"
    )
    Page<AdhesionPiece> findAllWithToOneRelationships(Pageable pageable);

    @Query("select adhesionPiece from AdhesionPiece adhesionPiece left join fetch adhesionPiece.institution")
    List<AdhesionPiece> findAllWithToOneRelationships();

    @Query("select adhesionPiece from AdhesionPiece adhesionPiece left join fetch adhesionPiece.institution where adhesionPiece.id =:id")
    Optional<AdhesionPiece> findOneWithToOneRelationships(@Param("id") UUID id);

    List<AdhesionPiece> findByInstitutionIdOrderByCode(UUID institutionId);

    Optional<AdhesionPiece> findByInstitutionIdAndCode(UUID institutionId, String code);

    long countByStateAndInstitutionStatus(AdhesionPieceState state, cm.gimac.des.participant.domain.enumeration.InstitutionStatus status);
}
