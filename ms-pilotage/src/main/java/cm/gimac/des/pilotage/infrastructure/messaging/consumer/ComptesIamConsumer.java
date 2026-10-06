package cm.gimac.des.pilotage.infrastructure.messaging.consumer;

import cm.gimac.des.pilotage.application.port.in.InstancesProcessusUseCase;
import cm.gimac.des.pilotage.domain.ProcessedEvent;
import cm.gimac.des.pilotage.repository.ProcessedEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Départ ou désactivation d'un compte (processus d'unité, D-87 §2) : sujet iam.account de MS-IAM, type
 * iam.account-administered.v1 (charge {@code { action, compteId, identifiant, acteurId, motif, details }}) avec
 * l'action DESACTIVATION ou SUPPRESSION (clé et subject : identifiant du compte). Les étapes en cours du compte, ses signatures et validations attendues
 * passent au profil de remplacement. Consommation idempotente par l'identifiant CloudEvents (table
 * processed_event) ; un message illisible est journalisé et ignoré ; une erreur de traitement (MS-IAM
 * injoignable) remonte pour que Kafka le rejoue.
 */
@Component
public class ComptesIamConsumer {

    static final String GROUPE = "pilotage-processus-comptes";
    static final String ADMINISTRE = "iam.account-administered.v1";
    static final Set<String> DEPARTS = Set.of("DESACTIVATION", "SUPPRESSION");
    private static final Logger LOG = LoggerFactory.getLogger(ComptesIamConsumer.class);

    private final InstancesProcessusUseCase processus;
    private final ProcessedEventRepository traites;
    private final ObjectMapper json;
    private final Clock horloge;

    public ComptesIamConsumer(InstancesProcessusUseCase processus, ProcessedEventRepository traites, ObjectMapper json, Clock horloge) {
        this.processus = processus;
        this.traites = traites;
        this.json = json;
        this.horloge = horloge;
    }

    @KafkaListener(
        id = GROUPE,
        topics = "${gimac.consommateurs.comptes.sujet:iam.account}",
        groupId = GROUPE,
        autoStartup = "${gimac.consommateurs.demarrage-automatique:true}",
        properties = { "auto.offset.reset=earliest" }
    )
    @Transactional
    public void recevoir(String message) {
        JsonNode e;
        try {
            e = json.readTree(message);
        } catch (JsonProcessingException ex) {
            LOG.warn("Message illisible sur le sujet des comptes : ignoré");
            return;
        }
        String type = e.path("type").asText("");
        Optional<String> compte = depart(type, e.path("data"));
        if (compte.isEmpty()) {
            return;
        }
        Optional<UUID> id = uuid(e.path("id").asText(null));
        if (id.isPresent() && traites.existsByEventId(id.orElseThrow())) {
            LOG.debug("Événement {} déjà traité", id.orElseThrow());
            return;
        }
        int n = processus.traiterDepart(compte.orElseThrow(), texte(e.path("data"), "acteurId"));
        id.ifPresent(x -> traites.save(new ProcessedEvent().eventId(x).subject(type).processedAt(horloge.instant())));
        LOG.info("Départ d'un compte : {} étape(s) de processus réaffectée(s)", n);
    }

    /** Compte parti : désactivé ou supprimé par l'administration. */
    static Optional<String> depart(String type, JsonNode d) {
        String compte = texte(d, "compteId");
        if (compte == null) {
            return Optional.empty();
        }
        if (ADMINISTRE.equals(type) && DEPARTS.contains(String.valueOf(texte(d, "action")))) {
            return Optional.of(compte);
        }
        return Optional.empty();
    }

    private static String texte(JsonNode d, String champ) {
        JsonNode n = d.path(champ);
        return n.isMissingNode() || n.isNull() || n.asText().isBlank() ? null : n.asText();
    }

    private static Optional<UUID> uuid(String s) {
        try {
            return s == null ? Optional.empty() : Optional.of(UUID.fromString(s));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
