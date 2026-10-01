package cm.gimac.des.gateway.domain.appareils;

import cm.gimac.des.gateway.domain.connexion.CauseEchec;
import java.util.Optional;

/** Résultat d'une tentative de connexion inscrit à l'historique (docs/lot1/mon-compte.md §3). */
public enum ResultatTentative {
    REUSSIE,
    ECHEC_MOT_DE_PASSE,
    ECHEC_CODE,
    BLOQUEE;

    /**
     * Résultat d'un échec : identifiants refusés (mot de passe, ou code si le mot de passe était juste),
     * tentative bloquée (limitation, compte inactif, couche refusée) ; vide pour une requête mal formée
     * ou un service indisponible, qui ne sont pas des tentatives de l'utilisateur.
     */
    public static Optional<ResultatTentative> pour(CauseEchec cause, boolean etapeCode) {
        return switch (cause) {
            case IDENTIFIANTS_INVALIDES -> Optional.of(etapeCode ? ECHEC_CODE : ECHEC_MOT_DE_PASSE);
            case TROP_DE_TENTATIVES, COMPTE_INACTIF, COUCHE_REFUSEE -> Optional.of(BLOQUEE);
            default -> Optional.empty();
        };
    }
}
