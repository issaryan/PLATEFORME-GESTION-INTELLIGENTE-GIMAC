package cm.gimac.des.pilotage.domain.processus;

/**
 * État d'une étape d'instance (§2) : EN_ATTENTE (prédécesseurs non terminés), ACTIVE (dépôts et champs),
 * A_SIGNER (signatures dans l'ordre), A_VALIDER (validateur), TERMINEE, ANNULEE.
 */
public enum EtatEtape {
    EN_ATTENTE,
    ACTIVE,
    A_SIGNER,
    A_VALIDER,
    TERMINEE,
    ANNULEE;

    /** Étape en cours de traitement : sa tâche est ouverte. */
    public boolean enCours() {
        return this == ACTIVE || this == A_SIGNER || this == A_VALIDER;
    }

    public boolean estFinal() {
        return this == TERMINEE || this == ANNULEE;
    }
}
