package cm.gimac.des.gateway.domain.connexion;

/** Échec d'une étape de la connexion ; la cause porte le statut HTTP et le code du contrat. */
public class EchecConnexion extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final CauseEchec cause;
    /** Compte identifié avant l'échec (mot de passe juste), nul sinon. */
    private final String sujet;
    /** Échec à l'étape du code (second facteur), le mot de passe étant juste. */
    private final boolean etapeCode;

    public EchecConnexion(CauseEchec cause) {
        this(cause, cause.message());
    }

    /** Détail technique réservé au journal, jamais renvoyé au navigateur. */
    public EchecConnexion(CauseEchec cause, String detailTechnique) {
        this(cause, detailTechnique, null, false);
    }

    public EchecConnexion(CauseEchec cause, String detailTechnique, String sujet, boolean etapeCode) {
        super(detailTechnique, null, false, false);
        this.cause = cause;
        this.sujet = sujet;
        this.etapeCode = etapeCode;
    }

    /** Même échec rattaché au compte identifié (historique des connexions). */
    public EchecConnexion pour(String sujetIdentifie) {
        return sujet != null ? this : new EchecConnexion(cause, getMessage(), sujetIdentifie, etapeCode);
    }

    public String sujet() {
        return sujet;
    }

    public boolean etapeCode() {
        return etapeCode;
    }

    public CauseEchec causeEchec() {
        return cause;
    }
}
