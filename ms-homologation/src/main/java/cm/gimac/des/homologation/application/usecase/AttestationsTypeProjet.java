package cm.gimac.des.homologation.application.usecase;

import cm.gimac.des.homologation.domain.DefinitionTypeProjet;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.ModeleTypeProjet;
import cm.gimac.des.homologation.repository.DefinitionTypeProjetRepository;
import org.springframework.stereotype.Component;

/**
 * Le modèle d'un type de projet (D-91, données) exige-t-il des attestations du participant ? Sert au contrôle de
 * publication : les gestes d'attestation (dossier de changement, archivage des attestations) ne sont attendus que dans
 * ce cas (GESTE_CERTIFICATION_ABSENT, docs/lot1/effet-reel-referentiels.md §10.5).
 */
@Component
public class AttestationsTypeProjet {

    private final DefinitionTypeProjetRepository typesProjet;

    public AttestationsTypeProjet(DefinitionTypeProjetRepository typesProjet) {
        this.typesProjet = typesProjet;
    }

    public boolean exigees(TypeProjet t) {
        if (t == null) {
            return true;
        }
        ModeleTypeProjet m = typesProjet.findById(t.name()).map(DefinitionTypeProjet::getModele).orElseGet(() -> t.repris() ? ModeleTypeProjet.parDefaut(t) : null);
        return m == null || !m.attestationsExigees(false).isEmpty();
    }
}
