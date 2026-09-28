package cm.gimac.des.pilotage.domain.exception;

/**
 * Levée quand une transition est absente de la table transition_autorisee.
 * Mappée en HTTP 409 Conflict par TransitionController.
 */
public class TransitionInterditeException extends RuntimeException {

    private final String dimension;
    private final String de;
    private final String vers;

    public TransitionInterditeException(String dimension, String de, String vers) {
        super("Transition interdite sur " + dimension + " : " + de + " → " + vers);
        this.dimension = dimension;
        this.de        = de;
        this.vers      = vers;
    }

    public String getDimension() { return dimension; }
    public String getDe()        { return de; }
    public String getVers()      { return vers; }
}
