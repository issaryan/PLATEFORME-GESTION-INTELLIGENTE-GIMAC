package cm.gimac.des.homologation.infrastructure.messaging.consumer;

import cm.gimac.des.homologation.application.usecase.sandbox.GelLiaisonsInstitution;
import cm.gimac.des.homologation.infrastructure.messaging.Json;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

/**
 * participant.status-changed.v1 (sujet participant.status, MS-PARTICIPANT) : gel et rétablissement des liaisons de
 * l'institution (HOM-06, D-122). Consommation idempotente par identifiant d'événement (table des événements traités) ;
 * {@code versionStatutRegistre} (D-120) est facultatif : absent, nul ou à 0, l'ordre est celui de {@code dateEffet}.
 * Un message illisible part en rebut par le gestionnaire d'erreurs commun.
 */
@Component
public class StatutsInstitutionConsumer {

    static final String TYPE = "participant.status-changed.v1";
    static final String GROUPE = "homologation-statut-institution";
    private static final Logger LOG = LoggerFactory.getLogger(StatutsInstitutionConsumer.class);

    private final IdempotentConsumer idempotent;
    private final GelLiaisonsInstitution gel;

    public StatutsInstitutionConsumer(IdempotentConsumer idempotent, GelLiaisonsInstitution gel) {
        this.idempotent = idempotent;
        this.gel = gel;
    }

    @KafkaListener(
        id = GROUPE,
        topics = "${gimac.consommateurs.statut-institution.sujet:participant.status}",
        groupId = GROUPE,
        autoStartup = "${gimac.consommateurs.demarrage-automatique:true}"
    )
    @Transactional
    public void recevoir(String message) {
        JsonNode e = Json.tree(message);
        String id = e.path("id").asString();
        if (!TYPE.equals(e.path("type").asString()) || id == null || id.isBlank()) {
            return;
        }
        JsonNode d = e.path("data");
        String institution = d.path("institutionId").asString();
        String statut = d.path("statut").asString();
        if (institution == null || institution.isBlank() || statut == null || statut.isBlank()) {
            LOG.warn("Événement {} sans institution ni statut : ignoré", id);
            return;
        }
        long v = d.path("versionStatutRegistre").asLong(0);
        Long version = v > 0 ? v : null;
        String effet = d.path("dateEffet").asString();
        Instant date = effet == null || effet.isBlank() ? null : Instant.parse(effet);
        String motif = d.path("motif").isString() ? d.path("motif").asString() : null;
        if (!idempotent.handleOnce(id, GROUPE, () -> gel.appliquer(UUID.fromString(institution), statut, version, date, motif))) {
            LOG.debug("Événement {} déjà traité", id);
        }
    }
}
