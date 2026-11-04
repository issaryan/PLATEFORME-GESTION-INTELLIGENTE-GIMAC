package cm.gimac.des.certification.application.usecase.sandbox;

import cm.gimac.des.certification.domain.CertificationCircuit;
import cm.gimac.des.certification.domain.CertificationException;
import cm.gimac.des.certification.domain.Quitus;
import cm.gimac.des.certification.domain.enumeration.EtatCircuit;
import cm.gimac.des.certification.infrastructure.persistence.CircuitsLiaisonRepository;
import cm.gimac.des.certification.repository.QuitusRepository;
import cm.gimac.des.certification.service.SignedDecisionService;
import cm.gimac.des.certification.service.dto.SignedDecisionDTO;
import cm.gimac.des.certification.service.impl.SignedDecisionServiceImpl;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CER-06, US-EXI-25 : une décision ne signe que le socle d'évaluation du circuit actuellement ouvert de sa liaison ;
 * sinon CER_SOCLE_OUTDATED (409) avec l'empreinte à jour. Contrôle posé devant le service généré
 * SignedDecisionServiceImpl, qui n'est pas édité (D-48) ; les autres opérations lui sont déléguées telles quelles.
 */
@Service
@Primary
@Transactional
public class ControleSocleDecisions implements SignedDecisionService {

    private final SignedDecisionServiceImpl genere;
    private final QuitusRepository quitus;
    private final CircuitsLiaisonRepository circuits;

    public ControleSocleDecisions(SignedDecisionServiceImpl genere, QuitusRepository quitus, CircuitsLiaisonRepository circuits) {
        this.genere = genere;
        this.quitus = quitus;
        this.circuits = circuits;
    }

    @Override
    public SignedDecisionDTO save(SignedDecisionDTO decision) {
        verifierSocle(decision);
        return genere.save(decision);
    }

    private void verifierSocle(SignedDecisionDTO decision) {
        Quitus q = quitus
            .findById(decision.getQuitus().getId())
            .orElseThrow(() -> CertificationException.introuvable("CER_QUITUS_NOT_FOUND", "Le quitus à signer est introuvable."));
        CertificationCircuit circuit = q.getCircuit();
        CertificationCircuit courant = circuits.findFirstByLiaisonIdOrderByCycleDesc(circuit.getLiaisonId()).orElse(circuit);
        boolean surCircuitCourant = courant.getId().equals(circuit.getId()) && circuit.getEtat() == EtatCircuit.OUVERT;
        if (!surCircuitCourant || !courant.getSocleEmpreinte().equals(decision.getSocleEmpreinte())) {
            throw CertificationException.conflit(
                "CER_SOCLE_OUTDATED",
                "Le socle d'évaluation a changé depuis son affichage.",
                Map.of("socleAJour", courant.getSocleEmpreinte())
            );
        }
    }

    @Override
    public SignedDecisionDTO update(SignedDecisionDTO decision) {
        return genere.update(decision);
    }

    @Override
    public Optional<SignedDecisionDTO> partialUpdate(SignedDecisionDTO decision) {
        return genere.partialUpdate(decision);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SignedDecisionDTO> findAll() {
        return genere.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SignedDecisionDTO> findOne(UUID id) {
        return genere.findOne(id);
    }

    @Override
    public void delete(UUID id) {
        genere.delete(id);
    }
}
