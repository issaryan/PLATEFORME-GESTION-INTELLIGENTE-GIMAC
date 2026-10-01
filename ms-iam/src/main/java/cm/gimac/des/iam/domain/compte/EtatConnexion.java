package cm.gimac.des.iam.domain.compte;

import java.util.List;

/**
 * État de connexion d'un compte, lu par la passerelle après l'octroi direct (D-65) : compte actif,
 * code TOTP configuré, actions exigées par la plateforme (premier accès ou demande de l'administration).
 */
public record EtatConnexion(boolean actif, boolean secondFacteur, List<ActionRequise> actionsRequises) {
    public EtatConnexion {
        actionsRequises = actionsRequises == null ? List.of() : List.copyOf(actionsRequises);
    }

    public static EtatConnexion de(Compte c) {
        return new EtatConnexion(c.actif(), c.secondFacteur(), c.etapesRestantes());
    }
}
