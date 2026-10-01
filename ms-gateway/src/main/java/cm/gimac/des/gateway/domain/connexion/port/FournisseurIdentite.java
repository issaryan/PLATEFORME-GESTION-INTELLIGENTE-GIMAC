package cm.gimac.des.gateway.domain.connexion.port;

import cm.gimac.des.gateway.domain.connexion.ClientConnexion;
import cm.gimac.des.gateway.domain.connexion.Identite;
import cm.gimac.des.gateway.domain.connexion.Jetons;
import reactor.core.publisher.Mono;

/** Fournisseur d'identité (Keycloak, domaine gimac), appelé de serveur à serveur. */
public interface FournisseurIdentite {
    /**
     * Octroi direct (grant_type=password, code TOTP en paramètre otp s'il est fourni). Échecs :
     * EchecConnexion IDENTIFIANTS_INVALIDES, COMPTE_INACTIF (compte désactivé selon le fournisseur)
     * ou SERVICE_INDISPONIBLE.
     */
    Mono<Jetons> octroyer(ClientConnexion client, String identifiant, String motDePasse, String code);

    /** Validation du jeton d'identité (JWKS, émetteur, audience = client) et lecture du jeton d'accès. */
    Mono<Identite> valider(ClientConnexion client, Jetons jetons);

    /** Révocation d'un jeton de rafraîchissement du client de l'enregistrement donné ; n'échoue jamais. */
    Mono<Void> revoquer(String enregistrement, String jetonRafraichissement);
}
