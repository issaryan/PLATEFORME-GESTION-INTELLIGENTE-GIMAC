package cm.gimac.des.gateway.domain.live;

import java.util.Set;

/**
 * Événement métier reçu d'un service, réduit à ce qu'il faut pour le remettre : identifiant,
 * type et sujet CloudEvents, sujet fonctionnel (premier segment du sujet Kafka), institution
 * concernée et, pour un message de la messagerie, les comptes destinataires.
 *
 * @param institutionId institution portée par l'enveloppe (claim « institutionid »), null si aucune
 * @param destinataires identifiants IAM des destinataires ; vide si l'événement n'en désigne pas
 */
public record EvenementLive(String id, String type, String subject, String topic, String institutionId, Set<String> destinataires) {
    public static final String HOMOLOGATION = "homologation";
    public static final String CERTIFICATION = "certification";
    public static final String COLLABORATION = "collaboration";

    public EvenementLive {
        destinataires = destinataires == null ? Set.of() : Set.copyOf(destinataires);
    }

    /** Représentation remise au navigateur, sans institution ni destinataires. */
    public VueEvenement vue() {
        return new VueEvenement(id, type, subject, topic);
    }
}
