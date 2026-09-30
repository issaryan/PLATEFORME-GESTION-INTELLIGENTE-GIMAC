package cm.gimac.des.gateway.domain.live;

/**
 * Événement tel que reçu par le navigateur (contrat de messagerie §5) : identifiant, type et
 * sujet CloudEvents, sujet fonctionnel. Jamais de donnée métier : le frontend invalide ses
 * requêtes du sujet et relit l'état par l'API, sous ses propres droits.
 */
public record VueEvenement(String id, String type, String subject, String topic) {}
