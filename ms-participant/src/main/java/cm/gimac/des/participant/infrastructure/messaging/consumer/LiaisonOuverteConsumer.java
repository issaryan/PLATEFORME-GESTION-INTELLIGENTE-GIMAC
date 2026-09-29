package cm.gimac.des.participant.infrastructure.messaging.consumer;

import cm.gimac.des.participant.application.port.in.ManageInstitutionUseCase;
import cm.gimac.des.participant.infrastructure.messaging.Json;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

/**
 * Ouverture d'une liaison (homologation.liaison-opened.v1, sujet homologation.liaison) : l'adhérent
 * passe automatiquement à EN_HOMOLOGATION (étape 3, D-57). Consommation idempotente (RG-15, RG-35) ;
 * un message illisible part en rebut par le gestionnaire d'erreurs commun.
 */
@Component
public class LiaisonOuverteConsumer {

    static final String TYPE = "homologation.liaison-opened.v1";
    static final String GROUPE = "participant-liaison-ouverte";
    private static final Logger LOG = LoggerFactory.getLogger(LiaisonOuverteConsumer.class);

    private final IdempotentConsumer idempotent;
    private final ManageInstitutionUseCase institutions;

    public LiaisonOuverteConsumer(IdempotentConsumer idempotent, ManageInstitutionUseCase institutions) {
        this.idempotent = idempotent;
        this.institutions = institutions;
    }

    @KafkaListener(
        id = GROUPE,
        topics = "homologation.liaison",
        groupId = GROUPE,
        autoStartup = "${gimac.consommateurs.demarrage-automatique:true}"
    )
    @Transactional
    public void recevoir(String message) {
        JsonNode e = Json.tree(message);
        if (!TYPE.equals(e.path("type").asString())) {
            return;
        }
        JsonNode d = e.path("data");
        UUID institution = UUID.fromString(d.path("institutionId").asString());
        UUID liaison = UUID.fromString(d.path("liaisonId").asString());
        String typeProjet = d.path("typeProjet").asString();
        boolean traite = idempotent.handleOnce(e.path("id").asString(), GROUPE, () -> institutions.debuterHomologation(institution, liaison, typeProjet));
        if (!traite) {
            LOG.debug("Événement {} déjà traité", e.path("id").asString());
        }
    }
}
