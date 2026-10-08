package cm.gimac.des.notification.infrastructure.messaging.consumer;

import cm.gimac.des.notification.application.port.in.RecevoirHomologationUseCase;
import cm.gimac.des.notification.domain.service.ReglesHomologation.FaitsJalon;
import cm.gimac.des.notification.domain.service.ReglesHomologation.FaitsOperation;
import cm.gimac.des.notification.domain.service.ReglesHomologation.Intervenant;
import cm.gimac.des.notification.domain.service.ReglesHomologation.Nature;
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
 * Événements du parcours d'homologation transformés en notifications HOMOLOGATION (D-93,
 * docs/lot1/effet-reel-referentiels.md §8.2 et §11), enveloppes CloudEvents 1.0 : homologation.operation-activated.v1
 * et homologation.operation-validation-requested.v1 (sujet homologation.operation), homologation.liaison-milestone-reached.v1
 * (sujet homologation.liaison). Les autres types de ces sujets (opération signée, liaison ouverte…) sont ignorés.
 * Consommation idempotente par identifiant d'événement (table processed_event, groupe propre) ; un message illisible
 * ou un MS-IAM injoignable part en rejeu puis en rebut par le gestionnaire d'erreurs commun.
 */
@Component
public class EvenementsHomologationConsumer {

    public static final String GROUPE = "notification-homologation";
    public static final String OPERATION_ACTIVEE = "homologation.operation-activated.v1";
    public static final String VALIDATION_DEMANDEE = "homologation.operation-validation-requested.v1";
    public static final String JALON_ATTEINT = "homologation.liaison-milestone-reached.v1";

    private static final Logger LOG = LoggerFactory.getLogger(EvenementsHomologationConsumer.class);

    private final IdempotentConsumer idempotent;
    private final RecevoirHomologationUseCase homologation;
    private final ObjectMapper json;

    public EvenementsHomologationConsumer(IdempotentConsumer idempotent, RecevoirHomologationUseCase homologation, ObjectMapper json) {
        this.idempotent = idempotent;
        this.homologation = homologation;
        this.json = json;
    }

    @KafkaListener(
        id = GROUPE,
        topics = "#{'${gimac.consommateurs.homologation.sujets:homologation.operation,homologation.liaison}'.split(',')}",
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
            case OPERATION_ACTIVEE -> operation(d, Nature.OPERATION_ACTIVEE);
            case VALIDATION_DEMANDEE -> operation(d, Nature.VALIDATION_DEMANDEE);
            case JALON_ATTEINT -> {
                FaitsJalon f = new FaitsJalon(
                    texte(d, "liaisonId"),
                    texte(d, "reference"),
                    texte(d, "institutionId"),
                    texte(d, "institutionNom"),
                    texte(d, "jalon"),
                    texte(d, "libelleJalon"),
                    texte(d, "conducteurId"),
                    intervenants(d.path("destinataires")),
                    texte(d, "lien")
                );
                Instant le = instant(d, "le");
                yield () -> homologation.jalonHomologation(f, le);
            }
            default -> null;
        };
    }

    private Runnable operation(JsonNode d, Nature nature) {
        FaitsOperation f = new FaitsOperation(
            nature,
            texte(d, "liaisonId"),
            texte(d, "reference"),
            texte(d, "institutionNom"),
            texte(d, "operationId"),
            texte(d, "code"),
            texte(d, "libelle"),
            texte(d, "geste"),
            date(d.get("echeance")),
            nature == Nature.OPERATION_ACTIVEE ? texte(d, "motif") : null,
            intervenant(d.get("responsable")),
            intervenant(d.get("validateur")),
            intervenants(d.path("destinataires")),
            textes(d.path("institutionsConcernees")),
            texte(d, "lien")
        );
        Instant le = instant(d, "le");
        return () -> homologation.operationHomologation(f, le);
    }

    private static Intervenant intervenant(JsonNode n) {
        if (n == null || !n.isObject()) {
            return null;
        }
        return new Intervenant(texte(n, "qualite"), texte(n, "fonction"), texte(n, "compteId"), n.path("aAffecter").asBoolean(false), texte(n, "institutionId"));
    }

    private static List<Intervenant> intervenants(JsonNode n) {
        List<Intervenant> l = new ArrayList<>();
        if (n != null && n.isArray()) {
            n.forEach(x -> {
                Intervenant i = intervenant(x);
                if (i != null) {
                    l.add(i);
                }
            });
        }
        return l;
    }

    /** Date AAAA-MM-JJ, ou tableau [AAAA, MM, JJ] (sérialisation numérique de Jackson) ; sinon null. */
    private static String date(JsonNode v) {
        if (v == null || v.isNull()) {
            return null;
        }
        if (v.isArray() && v.size() == 3) {
            return "%04d-%02d-%02d".formatted(v.get(0).asInt(), v.get(1).asInt(), v.get(2).asInt());
        }
        return v.isTextual() && !v.asText().isBlank() ? v.asText().strip() : null;
    }

    private static String texte(JsonNode n, String champ) {
        JsonNode v = n == null ? null : n.get(champ);
        return v == null || v.isNull() || v.isContainerNode() || v.asText().isBlank() ? null : v.asText();
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

    /** Instant ISO-8601, ou secondes depuis l'époque ; illisible : null (l'instant de réception le remplace). */
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
