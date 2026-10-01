package cm.gimac.des.gateway.domain.connexion.port;

import cm.gimac.des.gateway.domain.connexion.CauseEchec;
import cm.gimac.des.gateway.domain.connexion.DemandeConnexion;

/**
 * Journal d'audit de la connexion : gateway.login-succeeded.v1 et gateway.login-failed.v1, avec
 * l'identifiant masqué, l'adresse IP, la couche et la cause ; jamais le mot de passe ni le code.
 */
public interface JournalConnexion {
    String REUSSIE = "gateway.login-succeeded.v1";
    String ECHOUEE = "gateway.login-failed.v1";

    void reussie(DemandeConnexion demande, String sujet, String etat);

    void echouee(DemandeConnexion demande, CauseEchec cause, String detailTechnique);
}
