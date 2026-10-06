package cm.gimac.des.gateway.infrastructure.live;

import cm.gimac.des.gateway.domain.live.EvenementLive;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lecture d'une enveloppe CloudEvents 1.0 en mode structuré (PI §0.4 : specversion, id, source,
 * type, subject, time, correlationid, institutionid, data) publiée sur un sujet Kafka. Seuls
 * l'identifiant, le type, le sujet, l'institution et, pour la collaboration et les notifications de la
 * plateforme (D-80), les destinataires et l'auteur ou l'acteur sont retenus ; pour un accusé de lecture
 * (D-88), le lecteur et l'instant « luJusquA ». Le reste de « data » n'est jamais lu ni relayé.
 */
public class LectureCloudEvent {

    private static final Logger LOG = LoggerFactory.getLogger(LectureCloudEvent.class);

    private final ObjectMapper json;

    public LectureCloudEvent() {
        this(JsonMapper.builder().build());
    }

    public LectureCloudEvent(ObjectMapper json) {
        this.json = json;
    }

    /** Événement lu, vide si la valeur n'est pas une enveloppe exploitable (identifiant et type exigés). */
    public Optional<EvenementLive> lire(String sujetKafka, String valeur) {
        if (sujetKafka == null || valeur == null || valeur.isBlank()) {
            return Optional.empty();
        }
        JsonNode ce = arbre(valeur);
        if (ce == null || !ce.isObject()) {
            LOG.debug("Valeur ignorée sur {} : pas une enveloppe CloudEvents", sujetKafka);
            return Optional.empty();
        }
        String id = texte(ce, "id");
        String type = texte(ce, "type");
        if (id == null || type == null) {
            LOG.debug("Enveloppe ignorée sur {} : identifiant ou type absent", sujetKafka);
            return Optional.empty();
        }
        String topic = topic(sujetKafka);
        boolean collaboration = EvenementLive.COLLABORATION.equals(topic);
        boolean notification = EvenementLive.NOTIFICATION.equals(topic);
        JsonNode data = collaboration || notification ? donnees(ce.get("data")) : null;
        Set<String> destinataires = data != null ? destinataires(data) : Set.of();
        boolean lecture = collaboration && EvenementLive.LECTURE.equals(type);
        String acteur = data == null ? null : texte(data, champActeur(collaboration, lecture));
        String luJusquA = lecture && data != null ? texte(data, "luJusquA") : null;
        return Optional.of(new EvenementLive(id, type, texte(ce, "subject"), topic, texte(ce, "institutionid"), destinataires, acteur, luJusquA));
    }

    /** Champ de « data » qui désigne l'acteur : lecteur (D-88), auteur (messagerie) ou acteur (notification). */
    private static String champActeur(boolean collaboration, boolean lecture) {
        if (lecture) {
            return "lecteurId";
        }
        return collaboration ? "auteurId" : "acteurId";
    }

    /** Sujet fonctionnel : premier segment du sujet Kafka (homologation.liaison → homologation). */
    static String topic(String sujetKafka) {
        int point = sujetKafka.indexOf('.');
        return point < 0 ? sujetKafka : sujetKafka.substring(0, point);
    }

    /** « data » d'une enveloppe : un objet, ou une chaîne JSON ; null sinon. */
    private JsonNode donnees(JsonNode data) {
        JsonNode d = data != null && data.isString() ? arbre(data.asString()) : data;
        return d != null && d.isObject() ? d : null;
    }

    /** Destinataires de data.destinataires. */
    private Set<String> destinataires(JsonNode d) {
        Set<String> ids = new LinkedHashSet<>();
        if (d != null && d.isObject() && d.get("destinataires") != null && d.get("destinataires").isArray()) {
            for (JsonNode n : d.get("destinataires")) {
                if (n.isString() && !n.asString().isBlank()) {
                    ids.add(n.asString());
                }
            }
        }
        return ids;
    }

    private JsonNode arbre(String valeur) {
        try {
            return json.readTree(valeur);
        } catch (JacksonException e) {
            return null;
        }
    }

    private static String texte(JsonNode ce, String champ) {
        JsonNode n = ce.get(champ);
        return n == null || n.isNull() || n.asString().isBlank() ? null : n.asString();
    }
}
