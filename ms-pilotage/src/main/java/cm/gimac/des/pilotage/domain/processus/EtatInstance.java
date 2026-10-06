package cm.gimac.des.pilotage.domain.processus;

/** État d'une instance lancée : EN_COURS, SUSPENDUE (chef d'unité), CLOSE (toutes étapes terminées), ANNULEE. */
public enum EtatInstance {
    EN_COURS,
    SUSPENDUE,
    CLOSE,
    ANNULEE;

    public boolean estFinal() {
        return this == CLOSE || this == ANNULEE;
    }
}
