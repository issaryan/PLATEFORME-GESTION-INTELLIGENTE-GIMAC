package cm.gimac.des.gateway.domain.connexion;

import java.util.List;

/**
 * État de connexion d'un compte selon MS-IAM (GET /api/v1/interne/comptes/{sub}/etat-connexion).
 *
 * @param actif compte actif sur la plateforme
 * @param secondFacteur code TOTP configuré
 * @param actionsRequises actions exigées par la plateforme (MOT_DE_PASSE, SECOND_FACTEUR, FICHE, CHARTE, VERIFIER_COURRIEL)
 */
public record EtatCompte(boolean actif, boolean secondFacteur, List<String> actionsRequises) {
    public EtatCompte {
        actionsRequises = actionsRequises == null ? List.of() : actionsRequises.stream().filter(java.util.Objects::nonNull).toList();
    }
}
