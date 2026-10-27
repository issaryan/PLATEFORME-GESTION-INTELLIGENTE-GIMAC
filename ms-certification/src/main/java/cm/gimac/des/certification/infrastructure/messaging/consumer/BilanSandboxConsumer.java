package cm.gimac.des.certification.infrastructure.messaging.consumer;

import cm.gimac.des.certification.application.usecase.sandbox.SocleEvaluationSandbox;
import cm.gimac.des.certification.infrastructure.messaging.Json;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

/** Bilan Sandbox repris par MS-HOMOLOGATION (US-EXI-25), consommé une seule fois par identifiant CloudEvent. */
@Component
public class BilanSandboxConsumer {

    static final String TYPE = "homologation.campaign-result-imported.v1";
    static final String GROUPE = "certification-bilan-sandbox";
    private static final Logger LOG = LoggerFactory.getLogger(BilanSandboxConsumer.class);

    private final IdempotentConsumer idempotent;
    private final SocleEvaluationSandbox socles;

    public BilanSandboxConsumer(IdempotentConsumer idempotent, SocleEvaluationSandbox socles) {
        this.idempotent = idempotent;
        this.socles = socles;
    }

    @KafkaListener(
        id = GROUPE,
        topics = "homologation.campaign",
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
        UUID liaison = UUID.fromString(d.path("liaisonId").asString());
        Integer version = d.path("versionBilan").isNumber() ? d.path("versionBilan").asInt() : null;
        String empreinte = d.path("empreinteBilan").isTextual() ? d.path("empreinteBilan").asString() : null;
        boolean traite = idempotent.handleOnce(e.path("id").asString(), GROUPE, () -> socles.reprendre(liaison, version, empreinte));
        if (!traite) {
            LOG.debug("Événement {} déjà traité", e.path("id").asString());
        }
    }
}
