package cm.gimac.des.gateway.domain.live;

import java.util.Set;

/**
 * Événement métier reçu d'un service, réduit à ce qu'il faut pour le remettre : identifiant,
 * type et sujet CloudEvents, sujet fonctionnel (premier segment du sujet Kafka), institution
 * concernée et, pour un message de la messagerie ou une notification, les comptes destinataires et son
 * auteur (ou acteur).
 *
 * @param institutionId institution portée par l'enveloppe (claim « institutionid »), null si aucune
 * @param destinataires identifiants IAM des destinataires ; vide si l'événement n'en désigne pas
 * @param acteur compte à l'origine de l'événement (auteur d'un message, compte qui écrit, lecteur d'une
 *        conversation, compte dont la présence change), null si l'événement n'en désigne pas
 * @param luJusquA accusé de lecture (D-88) : instant d'envoi du dernier message lu (ISO-8601), null pour
 *        tout autre événement
 */
public record EvenementLive(
    String id,
    String type,
    String subject,
    String topic,
    String institutionId,
    Set<String> destinataires,
    String acteur,
    String luJusquA
) {
    public static final String HOMOLOGATION = "homologation";
    public static final String CERTIFICATION = "certification";
    public static final String COLLABORATION = "collaboration";
    /** Notifications de la plateforme (D-80, sujet Kafka notification.inapp) : remises au seul destinataire. */
    public static final String NOTIFICATION = "notification";
    /** Présence des comptes internes (D-72) : produite par la passerelle elle-même. */
    public static final String PRESENCE = "presence";

    /** « En train d'écrire » (D-72), signal de MS-COLLABORATION. */
    public static final String SAISIE = "collaboration.message-typing.v1";
    /** Accusé de lecture (D-88), MS-COLLABORATION : la position de lecture d'un membre a avancé. */
    public static final String LECTURE = "collaboration.conversation-read.v1";
    public static final String EN_LIGNE = "presence.account-online.v1";
    public static final String HORS_LIGNE = "presence.account-offline.v1";

    public EvenementLive {
        destinataires = destinataires == null ? Set.of() : Set.copyOf(destinataires);
    }

    public EvenementLive(String id, String type, String subject, String topic, String institutionId, Set<String> destinataires, String acteur) {
        this(id, type, subject, topic, institutionId, destinataires, acteur, null);
    }

    public EvenementLive(String id, String type, String subject, String topic, String institutionId, Set<String> destinataires) {
        this(id, type, subject, topic, institutionId, destinataires, null);
    }

    /**
     * Signal éphémère (D-72) : saisie et présence. Ni placé dans le tampon de reprise ni porteur d'un
     * identifiant de reprise : rejoué après coup, il annoncerait un état périmé.
     */
    public boolean ephemere() {
        return SAISIE.equals(type) || PRESENCE.equals(topic);
    }

    /** Représentation remise au navigateur, sans institution ni destinataires. */
    public VueEvenement vue() {
        return new VueEvenement(id, type, subject, topic, acteur, luJusquA);
    }

    /** Représentation remise au portail : ni acteur ni accusé de lecture (aucune donnée interne du GIMAC). */
    public VueEvenement vueExterne() {
        return new VueEvenement(id, type, subject, topic);
    }
}
