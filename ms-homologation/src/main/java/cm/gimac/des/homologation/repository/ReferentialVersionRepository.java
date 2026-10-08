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

    java.util.List<ReferentialVersion> findAllByOrderByDateCreationDesc();

    java.util.List<ReferentialVersion> findByEtat(cm.gimac.des.homologation.domain.enumeration.EtatVersionReferentiel etat);

    /** Une version de ce type a été publiée (publiée ou retirée) : le code et le modèle du type sont figés (D-91). */
    @org.springframework.data.jpa.repository.Query("select count(v) > 0 from ReferentialVersion v where v.typeProjet = :type and v.numero is not null")
    boolean dejaPubliee(@org.springframework.data.repository.query.Param("type") cm.gimac.des.homologation.domain.enumeration.TypeProjet type);

    /** Opérations et étapes d'un brouillon supprimé (D-92). */
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query(value = "delete from reference_operation where version_id = :id", nativeQuery = true)
    void supprimerOperations(@org.springframework.data.repository.query.Param("id") UUID id);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query(value = "delete from referential_etape where version_id = :id", nativeQuery = true)
    void supprimerEtapes(@org.springframework.data.repository.query.Param("id") UUID id);
}
