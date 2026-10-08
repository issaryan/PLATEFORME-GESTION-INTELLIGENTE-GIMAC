package cm.gimac.des.pilotage.infrastructure.messaging.consumer;

import cm.gimac.des.pilotage.application.port.in.ApprobationsTransitionsUseCase;
import cm.gimac.des.pilotage.application.port.in.ApprobationsTransitionsUseCase.Proposition;
import cm.gimac.des.pilotage.domain.ProcessedEvent;
import cm.gimac.des.pilotage.domain.exception.PilotageException;
import cm.gimac.des.pilotage.repository.ProcessedEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Transitions de statut proposées, approuvées et refusées dans MS-PARTICIPANT (D-98, docs/lot1/lancement-processus.md
 * §8.8) : sujet participant.transition ({@code gimac.consommateurs.transitions.sujet}), clé = institution, donc dans
 * l'ordre pour une institution. Types : participant.transition-requested.v1 (tâche à prendre par les porteurs de
 * PAR_TRANSITION_APPROVE), participant.transition-approved.v1 ({@code definitive} : tâche terminée ; sinon remise à
 * prendre pour le second approbateur) et participant.transition-rejected.v1 (tâche terminée).
 *
 * <p>Consommation idempotente par l'identifiant CloudEvents (table processed_event, groupe
 * pilotage-transitions-participant) ; un message illisible ou incomplet est journalisé et ignoré ; un refus définitif
 * (4xx) est journalisé et marqué traité ; une indisponibilité (MS-IAM injoignable, 503) remonte pour que Kafka le rejoue.
 * Chaque traitement a sa propre transaction (celle du cas d'utilisation) : un refus ne rend pas la marque de traitement
 * impossible à enregistrer ; les effets sont idempotents (une tâche par proposition, clôture et remise à prendre sans
 * effet répétées).
 */
@Component
public class TransitionsParticipantConsumer {

    static final String GROUPE = "pilotage-transitions-participant";
    static final String PROPOSEE = "participant.transition-requested.v1";
    static final String APPROUVEE = "participant.transition-approved.v1";
    static final String REJETEE = "participant.transition-rejected.v1";
    private static final Logger LOG = LoggerFactory.getLogger(TransitionsParticipantConsumer.class);

    private final ApprobationsTransitionsUseCase approbations;
    private final ProcessedEventRepository traites;
    private final ObjectMapper json;
    private final Clock horloge;

    public TransitionsParticipantConsumer(ApprobationsTransitionsUseCase approbations, ProcessedEventRepository traites, ObjectMapper json, Clock horloge) {
        this.approbations = approbations;
        this.traites = traites;
        this.json = json;
        this.horloge = horloge;
    }

    @KafkaListener(
        id = GROUPE,
        topics = "${gimac.consommateurs.transitions.sujet:participant.transition}",
        groupId = GROUPE,
        autoStartup = "${gimac.consommateurs.demarrage-automatique:true}",
        properties = { "auto.offset.reset=earliest" }
    )
    public void recevoir(String message) {
        JsonNode e;
        try {
            e = json.readTree(message);
        } catch (JsonProcessingException ex) {
            LOG.warn("Message illisible sur le sujet des transitions : ignoré");
            return;
        }
        String type = e.path("type").asText("");
        if (!PROPOSEE.equals(type) && !APPROUVEE.equals(type) && !REJETEE.equals(type)) {
            return;
        }
        JsonNode d = e.path("data");
        String institution = texte(d, "institutionId");
        String transition = texte(d, "transitionId");
        if (institution == null || transition == null) {
            LOG.warn("Événement {} sans institution ni transition : ignoré", type);
            return;
        }
        Optional<UUID> id = uuid(e.path("id").asText(null));
        if (id.isPresent() && traites.existsByEventId(id.orElseThrow())) {
            LOG.debug("Événement {} déjà traité", id.orElseThrow());
            return;
        }
        try {
            traiter(type, d, institution, transition);
        } catch (PilotageException ex) {
            if (ex.status() >= 500) {
                throw ex;
            }
            // Refus définitif (requête invalide) : rejouer ne changerait rien.
            LOG.warn("Événement {} de la transition {} refusé : {} ({})", type, transition, ex.code(), ex.getMessage());
        }
        id.ifPresent(x -> traites.save(new ProcessedEvent().eventId(x).subject(type).processedAt(horloge.instant())));
    }

    private void traiter(String type, JsonNode d, String institution, String transition) {
        switch (type) {
            case PROPOSEE -> {
                boolean creee = approbations.proposee(
                    new Proposition(
                        institution,
                        transition,
                        texte(d, "de"),
                        texte(d, "vers"),
                        texte(d, "motif"),
                        texte(d, "demandeurId"),
                        d.path("approbationsRequises").asInt(1),
                        texte(d, "institutionNom"),
                        texte(d, "lien")
                    )
                );
                LOG.info("Transition {} proposée : tâche de l'approbateur {}", transition, creee ? "créée" : "déjà présente");
            }
            case APPROUVEE -> {
                boolean definitive = d.path("definitive").asBoolean(true);
                int n = approbations.approuvee(institution, transition, definitive);
                LOG.info("Transition {} approuvée{} : {} tâche(s) visée(s)", transition, definitive ? "" : " (première approbation)", n);
            }
            default -> {
                int n = approbations.rejetee(institution, transition);
                LOG.info("Transition {} refusée : {} tâche(s) visée(s)", transition, n);
            }
        }
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
