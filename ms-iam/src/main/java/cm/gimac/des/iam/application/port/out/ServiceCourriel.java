package cm.gimac.des.iam.application.port.out;

import cm.gimac.des.iam.domain.compte.Courriel;

/**
 * Envoi des courriels de la plateforme (D-65) avec les paramètres de messagerie de la console (D-62).
 * Adaptateur SMTP en production, adaptateur en mémoire en développement et en test.
 */
public interface ServiceCourriel {
    /** Envoie le courriel ; IamException 503 IAM_SMTP_FAILED si le serveur le refuse. */
    void envoyer(Courriel courriel);
}
