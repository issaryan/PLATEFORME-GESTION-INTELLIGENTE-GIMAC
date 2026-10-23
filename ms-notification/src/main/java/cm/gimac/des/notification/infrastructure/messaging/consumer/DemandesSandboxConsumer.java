package cm.gimac.des.notification.infrastructure.messaging.consumer;

import cm.gimac.des.notification.application.port.in.RecevoirNotificationsSandboxUseCase;
import cm.gimac.des.notification.application.port.in.RecevoirNotificationsSandboxUseCase.Demande;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Demandes de notification de la Sandbox GIMAC (US-EXI-09, docs/lot1/sandbox-existant-adapte.md §11) : enveloppes
 * CloudEvents 1.0 de type {@code sbx-<service>.notification-requested.v1}, publiées par les onze services sur le sujet
 * du même nom (source {@code /gimac/sandbox/sbx-<service>}). Données : {@code modele}, {@code destinataires} (comptes ou
 * {@code ROLE:<rôle>}), {@code codeParticipant}, {@code codeEspace}, {@code le} ; forme de SBX-DONNEES admise :
 * {@code gabarit} pour le modèle, {@code destinataire} unique (nom de rôle), code participant pris de l'extension
 * {@code partitionkey}. Consommation idempotente par
 * identifiant d'événement (table processed_event, groupe notification-sandbox) : une demande redélivrée n'est remise
 * qu'une fois. Un autre type est ignoré ; un message illisible ou MS-IAM injoignable part en rejeu puis en rebut
 * ({@code <sujet>.dlt}) par le gestionnaire d'erreurs commun.
 */
@Component
public class DemandesSandboxConsumer {

    public static final String GROUPE = "notification-sandbox";

    /** Sujets des onze services, à défaut de gimac.consommateurs.sandbox.sujets. */
    public static final String SUJETS =
        "sbx-espace.notification-requested.v1,sbx-raccordement.notification-requested.v1,sbx-referentiel.notification-requested.v1,sbx-donnees.notification-requested.v1,sbx-simulation.notification-requested.v1,sbx-mobile.notification-requested.v1,sbx-iso20022.notification-requested.v1,sbx-carte.notification-requested.v1,sbx-campagne.notification-requested.v1,sbx-trace.notification-requested.v1,sbx-lecture.notification-requested.v1";

    /** Type d'une demande de notification de la sandbox ; le groupe 1 est le service émetteur. */
    public static final Pattern TYPE = Pattern.compile("^(sbx-[a-z0-9]+)\\.notification-requested\\.v1$");

    private static final Logger LOG = LoggerFactory.getLogger(DemandesSandboxConsumer.class);

    private final IdempotentConsumer idempotent;
    private final RecevoirNotificationsSandboxUseCase sandbox;
    private final ObjectMapper json;

    public DemandesSandboxConsumer(IdempotentConsumer idempotent, RecevoirNotificationsSandboxUseCase sandbox, ObjectMapper json) {
        this.idempotent = idempotent;
        this.sandbox = sandbox;
        this.json = json;
    }

    @KafkaListener(
        id = GROUPE,
        topics = "#{'${gimac.consommateurs.sandbox.sujets:" + SUJETS + "}'.split(',')}",
        groupId = GROUPE,
        autoStartup = "${gimac.consommateurs.demarrage-automatique:true}"
    )
    @Transactional
    public void recevoir(String message) throws JsonProcessingException {
        JsonNode e = json.readTree(message);
        String type = texte(e, "type");
        String id = texte(e, "id");
        Matcher m = type == null ? null : TYPE.matcher(type);
        if (id == null || m == null || !m.matches()) {
            LOG.debug("Enveloppe sans identifiant ou d'un autre type : ignorée");
            return;
        }
        JsonNode d = e.path("data");
        if (d.isTextual()) {
            d = json.readTree(d.asText());
        }
        Demande demande = new Demande(
            id,
            m.group(1),
            premier(texte(d, "modele"), texte(d, "gabarit")),
            destinataires(d),
            premier(texte(d, "codeParticipant"), texte(e, "partitionkey")),
            texte(d, "codeEspace"),
            instant(d, "le")
        );
        if (!idempotent.handleOnce(id, GROUPE, () -> sandbox.demander(demande))) {
            LOG.debug("Demande {} déjà traitée", id);
        }
    }

    static String texte(JsonNode n, String champ) {
        JsonNode v = n.get(champ);
        return v == null || v.isNull() || v.isContainerNode() || v.asText().isBlank() ? null : v.asText();
    }

    private static String premier(String a, String b) {
        return a != null ? a : b;
    }

    /** Liste {@code destinataires} ; à défaut, {@code destinataire} unique : un nom de rôle devient {@code ROLE:<nom>}. */
    static List<String> destinataires(JsonNode d) {
        List<String> l = textes(d.path("destinataires"));
        String seul = texte(d, "destinataire");
        if (l.isEmpty() && seul != null) {
            l.add(seul.matches("[A-Z_]+") ? "ROLE:" + seul : seul);
        }
        return l;
    }

    private static List<String> textes(JsonNode n) {
        List<String> l = new ArrayList<>();
        if (n.isArray()) {
            n.forEach(x -> {
                if (x.isTextual() && !x.asText().isBlank()) {
                    l.add(x.asText());
                }
            });
        }
        return l;
    }

    /** Instant ISO-8601 ; illisible ou absent : null (l'instant de réception le remplace). */
    static Instant instant(JsonNode n, String champ) {
        String v = texte(n, champ);
        if (v == null) {
            return null;
        }
        try {
            return Instant.parse(v);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
