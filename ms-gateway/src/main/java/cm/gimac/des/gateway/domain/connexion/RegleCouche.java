package cm.gimac.des.gateway.domain.connexion;

import cm.gimac.des.gateway.domain.live.Couche;

/**
 * Couche ouverte par un compte (contrat D-65 §2, point 3) : un jeton portant institution_id
 * (correspondant d'un participant) n'ouvre que le portail ; sans institution_id (acteur interne),
 * seulement le département et la console.
 */
public final class RegleCouche {

    private RegleCouche() {}

    public static boolean autorise(Couche couche, String institutionId) {
        boolean participant = institutionId != null && !institutionId.isBlank();
        return participant == (couche == Couche.PORTAIL);
    }
}
