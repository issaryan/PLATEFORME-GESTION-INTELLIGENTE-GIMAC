package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.ReferentialVersion;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ReferentialVersion entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ReferentialVersionRepository extends JpaRepository<ReferentialVersion, UUID> {
    // HOM-01 : cycle de vie des versions du référentiel

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select v from ReferentialVersion v where v.id = :id")
    java.util.Optional<ReferentialVersion> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") java.util.UUID id);

    java.util.Optional<ReferentialVersion> findFirstByTypeProjetAndEtat(
        cm.gimac.des.homologation.domain.enumeration.TypeProjet typeProjet,
        cm.gimac.des.homologation.domain.enumeration.EtatVersionReferentiel etat
    );

    @org.springframework.data.jpa.repository.Query("select max(v.numero) from ReferentialVersion v where v.typeProjet = :type")
    java.util.Optional<Integer> maxNumero(@org.springframework.data.repository.query.Param("type") cm.gimac.des.homologation.domain.enumeration.TypeProjet type);

    java.util.List<ReferentialVersion> findByTypeProjetOrderByDateCreationDesc(cm.gimac.des.homologation.domain.enumeration.TypeProjet typeProjet);
}
