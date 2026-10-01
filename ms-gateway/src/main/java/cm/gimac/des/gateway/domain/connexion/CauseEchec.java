package cm.gimac.des.gateway.domain.connexion;

/**
 * Causes d'échec de la connexion et leur traduction HTTP (contrat D-65 §2 et §6). Le message reste
 * générique : il ne révèle jamais l'existence d'un compte ni la cause précise d'un refus
 * d'authentification.
 */
public enum CauseEchec {
    REQUETE_INVALIDE(400, "CNX_REQUETE_INVALIDE", "Requête de connexion incomplète ou mal formée."),
    IDENTIFIANTS_INVALIDES(401, "CNX_IDENTIFIANTS_INVALIDES", "Identifiant, mot de passe ou code incorrect."),
    COUCHE_REFUSEE(403, "CNX_COUCHE_REFUSEE", "Ce compte n'ouvre pas cette application."),
    COMPTE_INACTIF(403, "CNX_COMPTE_INACTIF", "Ce compte n'est pas actif."),
    TROP_DE_TENTATIVES(429, "CNX_TROP_DE_TENTATIVES", "Trop de tentatives : réessayez dans une minute."),
    SERVICE_INDISPONIBLE(503, "CNX_SERVICE_INDISPONIBLE", "Service de connexion momentanément indisponible."),
    SESSION_EXPIREE(401, "CNX_SESSION_EXPIREE", "Session fermée après inactivité ou durée maximale atteinte."),
    SESSION_RESTREINTE(403, "CNX_SESSION_RESTREINTE", "Terminez d'abord la configuration de votre compte.");

    private final int statut;
    private final String code;
    private final String message;

    CauseEchec(int statut, String code, String message) {
        this.statut = statut;
        this.code = code;
        this.message = message;
    }

    public int statut() {
        return statut;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
