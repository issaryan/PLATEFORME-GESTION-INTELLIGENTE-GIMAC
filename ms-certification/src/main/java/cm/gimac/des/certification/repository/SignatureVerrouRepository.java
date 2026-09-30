package cm.gimac.des.certification.repository;

import cm.gimac.des.certification.domain.decision.SignatureVerrou;
import cm.gimac.des.certification.domain.enumeration.ObjetSignature;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Signatures immuables des verrous (D-35) : insertion et lecture seulement. */
@Repository
public interface SignatureVerrouRepository extends JpaRepository<SignatureVerrou, UUID> {
    List<SignatureVerrou> findByLiaisonIdAndObjet(UUID liaisonId, ObjetSignature objet);
}
