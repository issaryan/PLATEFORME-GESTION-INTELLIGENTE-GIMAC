package cm.gimac.des.iam.domain.compte;

/**
 * Serveur d'envoi des courriels de la plateforme (invitations, réinitialisations), configuré
 * depuis la console. Le mot de passe n'est jamais relu : seul le fait qu'il est défini l'est.
 */
public record ConfigurationMessagerie(
    String hote,
    int port,
    String expediteur,
    String nomExpediteur,
    boolean starttls,
    boolean ssl,
    boolean authentification,
    String utilisateur,
    String motDePasse,
    boolean motDePasseDefini
) {
    /** Vue sans secret. */
    public ConfigurationMessagerie sansSecret() {
        return new ConfigurationMessagerie(hote, port, expediteur, nomExpediteur, starttls, ssl, authentification, utilisateur, null, motDePasseDefini);
    }
}
