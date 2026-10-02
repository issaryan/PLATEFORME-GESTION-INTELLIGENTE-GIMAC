package cm.gimac.des.gateway.domain.live;

/**
 * Événement tel que reçu par le navigateur (contrat de messagerie §5) : identifiant, type et
 * sujet CloudEvents, sujet fonctionnel et le compte à son origine (D-72), null s'il n'est pas désigné. Jamais
 * de donnée métier : le frontend invalide ses requêtes du sujet et relit l'état par l'API, sous
 * ses propres droits.
 */
public record VueEvenement(String id, String type, String subject, String topic, String acteur) {
    public VueEvenement(String id, String type, String subject, String topic) {
        this(id, type, subject, topic, null);
    }
}
