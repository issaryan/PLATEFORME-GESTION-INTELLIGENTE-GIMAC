package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.parcours.ParcoursLiaison;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/** État du parcours des liaisons (étapes 3 à 10). */
@Repository
public interface ParcoursLiaisonRepository extends JpaRepository<ParcoursLiaison, UUID> {
    /** Numéro suivant de la référence lisible LIA-AAAA-NNN. */
    @Query(value = "select nextval('parcours_reference_seq')", nativeQuery = true)
    long prochainNumero();
}
