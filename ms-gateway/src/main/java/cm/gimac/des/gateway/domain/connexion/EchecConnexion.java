package cm.gimac.des.gateway.domain.connexion;

/** Échec d'une étape de la connexion ; la cause porte le statut HTTP et le code du contrat. */
public class EchecConnexion extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final CauseEchec cause;

    public EchecConnexion(CauseEchec cause) {
        this(cause, cause.message());
    }

    /** Détail technique réservé au journal, jamais renvoyé au navigateur. */
    public EchecConnexion(CauseEchec cause, String detailTechnique) {
        super(detailTechnique, null, false, false);
        this.cause = cause;
    }

    public CauseEchec causeEchec() {
        return cause;
    }
}
