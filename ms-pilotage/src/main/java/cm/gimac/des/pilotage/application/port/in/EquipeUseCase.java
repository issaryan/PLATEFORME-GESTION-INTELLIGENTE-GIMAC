package cm.gimac.des.pilotage.application.port.in;

import cm.gimac.des.pilotage.application.port.in.AgendaUseCase.EvenementLu;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Membre;
import cm.gimac.des.pilotage.domain.activite.EtatRapport;
import java.util.List;

/** Vue d'équipe (§2.5) et synthèse du tableau de bord (§2.6). */
public interface EquipeUseCase {
    /** {@code rapportDuJour} null : aucun rapport quotidien (ABSENT). */
    record MembreDuJour(
        Membre membre,
        long prevues,
        long faites,
        long enCours,
        EtatRapport rapportDuJour,
        long tachesOuvertes,
        long tachesEnRetard,
        boolean absent
    ) {}

    record Synthese(
        long prevues,
        long faites,
        long enCours,
        List<EvenementLu> evenements,
        long tachesOuvertes,
        long tachesEnRetard,
        long echeanceProche,
        EtatRapport rapportDuJour,
        long rapportsARecevoir
    ) {}

    List<MembreDuJour> equipe(Appelant appelant, String jour);

    Synthese synthese(Appelant appelant, String jour);
}
