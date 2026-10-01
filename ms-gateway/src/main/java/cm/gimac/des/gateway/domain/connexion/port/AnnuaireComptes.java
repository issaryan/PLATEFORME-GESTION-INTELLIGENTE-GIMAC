package cm.gimac.des.gateway.domain.connexion.port;

import cm.gimac.des.gateway.domain.connexion.EtatCompte;
import reactor.core.publisher.Mono;

/** Routes internes de MS-IAM appelées par la passerelle avec son jeton de service (gateway-service). */
public interface AnnuaireComptes {
    /** Réponse d'une route de MS-IAM relayée telle quelle au navigateur (statut, type et corps). */
    record ReponseRelayee(int statut, String typeContenu, String corps) {}

    /** État de connexion du compte ; compte inconnu : EchecConnexion COMPTE_INACTIF ; panne : SERVICE_INDISPONIBLE. */
    Mono<EtatCompte> etat(String sujet);

    /** Demande de réinitialisation ; MS-IAM n'envoie le courriel que si le compte existe et est actif. */
    Mono<Void> motDePasseOublie(String identifiant, String couche);

    /** Nouveau mot de passe par jeton de lien ; 204 ou 400 (CNX_JETON_INVALIDE, IAM_PASSWORD_POLICY). */
    Mono<ReponseRelayee> nouveauMotDePasse(String jeton, String motDePasse);
}
