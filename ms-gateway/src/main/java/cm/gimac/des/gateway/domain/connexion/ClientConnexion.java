package cm.gimac.des.gateway.domain.connexion;

/**
 * Clients du fournisseur d'identité utilisés par la connexion, par leur enregistrement OAuth2 dans
 * la passerelle : « connexion » (client gimac-connexion, mot de passe seul, acr 1) et « oidc »
 * (client web_app, mot de passe et code TOTP obligatoire, acr 2).
 */
public enum ClientConnexion {
    MOT_DE_PASSE("connexion"),
    DEUX_FACTEURS("oidc");

    private final String enregistrement;

    ClientConnexion(String enregistrement) {
        this.enregistrement = enregistrement;
    }

    public String enregistrement() {
        return enregistrement;
    }
}
