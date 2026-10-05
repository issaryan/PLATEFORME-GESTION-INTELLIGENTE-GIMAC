package cm.gimac.des.notification.infrastructure.messaging.consumer;

import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.MessageEnvoye;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.RapportSoumis;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.TacheAffectee;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Événements transformés en notifications (docs/lot1/notifications.md §1), enveloppes CloudEvents 1.0 :
 * collaboration.message-sent.v1 (sujet collaboration.message), pilotage.task-assigned.v1 (sujet
 * pilotage.task) et pilotage.report-submitted.v1 (sujet pilotage.report) ; les autres types de ces sujets
 * sont ignorés. Consommation idempotente par identifiant d'événement (table processed_event, RG-15,
 * RG-35) ; un message illisible part en rebut par le gestionnaire d'erreurs commun.
 */
@Component
public class EvenementsNotificationsConsumer {

    public static final String GROUPE = "notification-inapp";
    public static final String MESSAGE_ENVOYE = "collaboration.message-sent.v1";
    public static final String TACHE_AFFECTEE = "pilotage.task-assigned.v1";
    public static final String RAPPORT_SOUMIS = "pilotage.report-submitted.v1";

    private static final Logger LOG = LoggerFactory.getLogger(EvenementsNotificationsConsumer.class);

    private final IdempotentConsumer idempotent;
    private final RecevoirEvenementsUseCase evenements;
    private final ObjectMapper json;

    public EvenementsNotificationsConsumer(IdempotentConsumer idempotent, RecevoirEvenementsUseCase evenements, ObjectMapper json) {
        this.idempotent = idempotent;
        this.evenements = evenements;
        this.json = json;
    }

    @KafkaListener(
        id = GROUPE,
        topics = "#{'${gimac.consommateurs.notification.sujets:collaboration.message,pilotage.task,pilotage.report}'.split(',')}",
        groupId = GROUPE,
        autoStartup = "${gimac.consommateurs.demarrage-automatique:true}"
    )
    @Transactional
    public void recevoir(String message) throws JsonProcessingException {
        JsonNode e = json.readTree(message);
        String type = texte(e, "type");
        String id = texte(e, "id");
        if (type == null || id == null) {
            LOG.debug("Enveloppe sans identifiant ou sans type : ignorée");
            return;
        }
        JsonNode d = e.path("data");
        if (d.isTextual()) {
            d = json.readTree(d.asText());
        }
        Runnable effet = effet(type, d);
        if (effet == null) {
            LOG.debug("Événement {} de type {} sans notification", id, type);
            return;
        }
        if (!idempotent.handleOnce(id, GROUPE, effet)) {
            LOG.debug("Événement {} déjà traité", id);
        }
    }

    private Runnable effet(String type, JsonNode d) {
        return switch (type) {
            case MESSAGE_ENVOYE -> () ->
                evenements.messageEnvoye(
                    new MessageEnvoye(texte(d, "conversationId"), texte(d, "messageId"), texte(d, "auteurId"), textes(d.path("destinataires")), instant(d, "envoyeLe"), texte(d, "evenement"))
                );
            case TACHE_AFFECTEE -> () ->
                evenements.tacheAffectee(new TacheAffectee(texte(d, "tacheId"), texte(d, "responsableId"), texte(d, "acteurId"), instant(d, "le"), texte(d, "titre"), texte(d, "echeance")));
            case RAPPORT_SOUMIS -> () ->
                evenements.rapportSoumis(
                    new RapportSoumis(
                        texte(d, "rapportId"),
                        texte(d, "type"),
                        texte(d, "periode"),
                        texte(d, "auteurId"),
                        texte(d, "destinataireId"),
                        texte(d, "acteurId"),
                        instant(d, "le")
                    )
                );
            default -> null;
        };
    }

    private static String texte(JsonNode n, String champ) {
        JsonNode v = n == null ? null : n.get(champ);
        return v == null || v.isNull() || v.asText().isBlank() ? null : v.asText();
    }

    private static List<String> textes(JsonNode n) {
        List<String> l = new ArrayList<>();
        if (n != null && n.isArray()) {
            n.forEach(x -> {
                if (x.isTextual() && !x.asText().isBlank()) {
                    l.add(x.asText());
                }
            });
        }
        return l;
    }

    /** Instant ISO-8601, ou secondes depuis l'époque (sérialisation numérique de Jackson) ; illisible : null. */
    private static Instant instant(JsonNode n, String champ) {
        JsonNode v = n == null ? null : n.get(champ);
        if (v == null || v.isNull()) {
            return null;
        }
        if (v.isNumber()) {
            return Instant.ofEpochSecond(v.longValue(), Math.round((v.doubleValue() - v.longValue()) * 1_000_000_000L));
        }
        try {
            return Instant.parse(v.asText());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
