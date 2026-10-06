package cm.gimac.des.gateway.domain.live;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Événement tel que reçu par le navigateur (contrat de messagerie §5) : identifiant, type et
 * sujet CloudEvents, sujet fonctionnel et le compte à son origine (D-72), null s'il n'est pas désigné. Jamais
 * de donnée métier : le frontend invalide ses requêtes du sujet et relit l'état par l'API, sous
 * ses propres droits. Seule exception, l'accusé de lecture (D-88) porte « luJusquA » (instant, sans
 * contenu), absent pour tout autre événement, pour mettre à jour les coches sans relire le fil.
 */
public record VueEvenement(
    String id,
    String type,
    String subject,
    String topic,
    String acteur,
    @JsonInclude(JsonInclude.Include.NON_NULL) String luJusquA
) {
    public VueEvenement(String id, String type, String subject, String topic, String acteur) {
        this(id, type, subject, topic, acteur, null);
    }

    public VueEvenement(String id, String type, String subject, String topic) {
        this(id, type, subject, topic, null);
    }
}
