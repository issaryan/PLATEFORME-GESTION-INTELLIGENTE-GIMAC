package cm.gimac.des.participant.application.port.in;

import cm.gimac.des.participant.application.port.in.ManageInstitutionUseCase.Acteur;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reprise des correspondants des institutions (D-132, extension de D-107, docs/exploitation/reprise-des-donnees.md §3
 * bis) : déclaration idempotente d'un correspondant actif, désigné par le code membre de son institution et le compte
 * MS-IAM qui le porte. C'est le correspondant que la Sandbox GIMAC exige pour ouvrir l'espace d'une institution
 * (SBX_ESP_CORRESPONDANT_INCONNU). En simulation, tout est contrôlé et rien n'est écrit.
 */
public interface RepriseCorrespondantsUseCase {
    /** Nombre maximal de lignes d'un lot. */
    int LIGNES_MAX = 2000;

    /**
     * Correspondant repris. {@code codeMembre}, {@code compteId} et {@code nom} sont obligatoires ; {@code fonction}
     * vaut MONETIQUE par défaut (DIRECTION, MONETIQUE, EXPLOITATION_24_7, SECURITE) ; {@code canaux} vaut
     * « messagerie » par défaut ; {@code astreinte247} vaut faux par défaut.
     */
    record CorrespondantRepris(
        String codeMembre,
        UUID compteId,
        String nom,
        String fonction,
        String email,
        String telephone,
        String canaux,
        Boolean astreinte247
    ) {}

    enum Resultat {
        CREE,
        MIS_A_JOUR,
        INCHANGE,
        REFUSE,
    }

    /** Ligne du rapport : résultat, champs changés ; code et motif d'un refus. */
    record Ligne(
        int rang,
        String codeMembre,
        UUID compteId,
        Resultat resultat,
        UUID id,
        List<String> changements,
        String code,
        String motif,
        Map<String, Object> details
    ) {}

    record Rapport(boolean simulation, int total, Map<Resultat, Long> totaux, List<Ligne> lignes) {}

    Rapport importer(List<CorrespondantRepris> lignes, boolean simulation, String motif, Acteur acteur);
}
