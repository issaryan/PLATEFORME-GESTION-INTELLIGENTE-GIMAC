package cm.gimac.des.homologation.infrastructure.messaging.consumer;

import cm.gimac.des.homologation.application.usecase.DepartsIntervenants;
import cm.gimac.des.homologation.infrastructure.messaging.Json;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

/**
 * Départ d'un compte (D-93 §1) : sujet iam.account de MS-IAM, type iam.account-administered.v1 (charge
 * {@code { action, compteId, identifiant, acteurId, motif, details }}), actions DESACTIVATION et SUPPRESSION. Les rôles
 * du compte sur les liaisons ouvertes repassent « à affecter », tâche au conducteur. Consommation idempotente par
 * l'identifiant CloudEvents ; un autre type ou une autre action est ignoré.
 */
@Component
public class ComptesIamConsumer {

    static final String GROUPE = "homologation-comptes";
    static final String ADMINISTRE = "iam.account-administered.v1";
    static final Set<String> DEPARTS = Set.of("DESACTIVATION", "SUPPRESSION");
    private static final Logger LOG = LoggerFactory.getLogger(ComptesIamConsumer.class);

    private final IdempotentConsumer idempotent;
    private final DepartsIntervenants departs;

    public ComptesIamConsumer(IdempotentConsumer idempotent, DepartsIntervenants departs) {
        this.idempotent = idempotent;
        this.departs = departs;
    }

    @KafkaListener(
        id = GROUPE,
        topics = "${gimac.consommateurs.comptes.sujet:iam.account}",
        groupId = GROUPE,
        autoStartup = "${gimac.consommateurs.demarrage-automatique:true}"
    )
    @Transactional
    public void recevoir(String message) {
        JsonNode e = Json.tree(message);
        String type = e.path("type").asString();
        String id = e.path("id").asString();
        JsonNode d = e.path("data");
        String compte = d.path("compteId").isMissingNode() || d.path("compteId").isNull() ? null : d.path("compteId").asString();
        String action = d.path("action").isMissingNode() || d.path("action").isNull() ? null : d.path("action").asString();
        if (!ADMINISTRE.equals(type) || id == null || id.isBlank() || compte == null || compte.isBlank() || action == null || !DEPARTS.contains(action)) {
            return;
        }
        if (!idempotent.handleOnce(id, GROUPE, () -> LOG.info("Départ du compte {} : {} liaison(s) touchée(s)", compte, departs.traiterDepart(compte)))) {
            LOG.debug("Événement {} déjà traité", id);
        }
    }
}
