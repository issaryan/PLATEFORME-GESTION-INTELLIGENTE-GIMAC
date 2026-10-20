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
}
