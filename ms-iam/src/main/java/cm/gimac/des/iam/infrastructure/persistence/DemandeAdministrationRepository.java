package cm.gimac.des.iam.infrastructure.persistence;

import cm.gimac.des.iam.domain.demande.EtatDemande;
import cm.gimac.des.iam.domain.demande.TypeDemande;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Demandes à l'administration (D-81), les plus récentes en tête. */
public interface DemandeAdministrationRepository extends JpaRepository<DemandeAdministration, UUID> {
    boolean existsByTypeAndCleCibleAndEtat(TypeDemande type, String cleCible, EtatDemande etat);

    Page<DemandeAdministration> findByDemandeurIdOrderByCreeeLeDesc(String demandeurId, Pageable page);

    Page<DemandeAdministration> findByDemandeurIdAndEtatOrderByCreeeLeDesc(String demandeurId, EtatDemande etat, Pageable page);

    Page<DemandeAdministration> findAllByOrderByCreeeLeDesc(Pageable page);

    Page<DemandeAdministration> findByEtatOrderByCreeeLeDesc(EtatDemande etat, Pageable page);
}
