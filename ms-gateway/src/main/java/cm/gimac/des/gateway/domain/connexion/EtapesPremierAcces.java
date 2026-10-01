package cm.gimac.des.gateway.domain.connexion;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Étapes du premier accès (contrat D-65 §2, point 5) : actions exigées par la plateforme, plus
 * SECOND_FACTEUR tant qu'aucun code TOTP n'est configuré. Ordre de l'assistant : mot de passe,
 * second facteur, fiche, charte, puis toute autre action dans l'ordre reçu.
 */
public final class EtapesPremierAcces {

    public static final String MOT_DE_PASSE = "MOT_DE_PASSE";
    public static final String SECOND_FACTEUR = "SECOND_FACTEUR";
    public static final String FICHE = "FICHE";
    public static final String CHARTE = "CHARTE";

    private static final List<String> ORDRE = List.of(MOT_DE_PASSE, SECOND_FACTEUR, FICHE, CHARTE);

    private EtapesPremierAcces() {}

    /** Étapes restantes ; liste vide si le compte peut ouvrir une session complète. */
    public static List<String> de(EtatCompte etat) {
        Set<String> demandees = new LinkedHashSet<>();
        for (String a : etat.actionsRequises()) {
            if (a != null && !a.isBlank()) {
                demandees.add(a.trim());
            }
        }
        if (!etat.secondFacteur()) {
            demandees.add(SECOND_FACTEUR);
        }
        List<String> etapes = new ArrayList<>();
        for (String e : ORDRE) {
            if (demandees.remove(e)) {
                etapes.add(e);
            }
        }
        etapes.addAll(demandees);
        return List.copyOf(etapes);
    }
}
