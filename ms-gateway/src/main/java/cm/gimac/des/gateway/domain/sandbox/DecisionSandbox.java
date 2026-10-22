package cm.gimac.des.gateway.domain.sandbox;

/**
 * Décision de la passerelle sur une route de la sandbox : admise ou refusée, institution au nom de laquelle l'appel est
 * relayé (vide pour un compte du GIMAC) et motif journalisé.
 */
public record DecisionSandbox(boolean admise, String institutionEffective, Motif motif) {

    public enum Motif {
        /** Compte du GIMAC (aucune institution dans le jeton) : console ou département. */
        COMPTE_GIMAC(true),
        /** Institution du jeton, aucune institution mandante demandée. */
        INSTITUTION_PROPRE(true),
        /** Institution mandante d'un mandat actif. */
        MANDAT_ACTIF(true),
        /** Institution demandée sans mandat pour ce compte. */
        MANDAT_ABSENT(false),
        /** Mandat de l'institution demandée échu. */
        MANDAT_ECHU(false),
        /** Mandat actif, mais aucun jeton interne réduit ne peut porter l'institution mandante (refus par défaut). */
        JETON_REDUIT_INDISPONIBLE(false),
        /** Quota de débit de la couche, de l'institution et de la route dépassé. */
        QUOTA_DEPASSE(false);

        private final boolean admis;

        Motif(boolean admis) {
            this.admis = admis;
        }

        public boolean admis() {
            return admis;
        }
    }

    public static DecisionSandbox de(Motif motif, String institution) {
        return new DecisionSandbox(motif.admis(), institution == null ? "" : institution, motif);
    }
}
