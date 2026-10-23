package cm.gimac.des.iam.application.port.out;

import java.util.Optional;

/**
 * Étapes de saga déjà appliquées (D-115, US-EXI-03 scénario 2) : une étape rejouée par SBX-ESPACE avec la même clé
 * d'idempotence rend le résultat d'origine sans rien refaire. Table etape_idempotente.
 */
public interface EtapesIdempotentes {
    /** Résultat enregistré pour cette clé et cette opération. */
    Optional<String> resultat(String cle, String operation);

    void enregistrer(String cle, String operation, String resultat, String acteur);

    /**
     * Réserve la clé de cette opération avec son résultat avant d'appliquer l'étape (D-119, US-EXI-06 scénario 4) : vide
     * si la réservation est faite maintenant, sinon le résultat déjà enregistré. L'adaptateur JDBC la rend atomique :
     * deux reprises concurrentes de la même étape n'en appliquent qu'une, la seconde rendant le résultat de la première.
     */
    default Optional<String> reserver(String cle, String operation, String resultat, String acteur) {
        Optional<String> deja = resultat(cle, operation);
        if (deja.isEmpty()) {
            enregistrer(cle, operation, resultat, acteur);
        }
        return deja;
    }
}
