package cm.gimac.des.gateway.domain.connexion;

/**
 * Réponse de jetons du fournisseur d'identité (octroi direct). Ne quitte jamais la passerelle.
 *
 * @param jetonAcces jeton d'accès (JWT)
 * @param jetonRafraichissement jeton de rafraîchissement, éventuellement absent
 * @param jetonIdentite jeton d'identité OIDC (portée openid)
 * @param dureeAcces durée de validité du jeton d'accès, en secondes
 * @param portee portées accordées, séparées par des espaces
 */
public record Jetons(String jetonAcces, String jetonRafraichissement, String jetonIdentite, long dureeAcces, String portee) {
    @Override
    public String toString() {
        // Aucun jeton dans les journaux.
        return "Jetons[dureeAcces=" + dureeAcces + ", portee=" + portee + "]";
    }
}
