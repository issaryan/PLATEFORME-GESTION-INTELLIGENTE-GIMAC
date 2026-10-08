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
 * Jalon du référentiel atteint par une liaison (homologation.liaison-milestone-reached.v1, sujet homologation.liaison,
 * D-93 §4) : au jalon EN_PRODUCTION, l'institution EN_HOMOLOGATION passe à ACTIF (transition automatique, journalisée,
 * participant.status-changed.v1 par l'outbox). Les autres jalons (APTE_CERTIFICATION, CERTIFIEE, APPROUVEE_SECURITE)
 * n'ont pas d'effet documenté sur l'institution : ignorés sans trace d'idempotence. Consommation idempotente (RG-15,
 * RG-35) ; un message illisible part en rebut par le gestionnaire d'erreurs commun.
 */
@Component
public class JalonLiaisonConsumer {

    static final String TYPE = "homologation.liaison-milestone-reached.v1";
    static final String GROUPE = "participant-jalon-liaison";
    static final String EN_PRODUCTION = "EN_PRODUCTION";
    private static final Logger LOG = LoggerFactory.getLogger(JalonLiaisonConsumer.class);

    private final IdempotentConsumer idempotent;
    private final ManageInstitutionUseCase institutions;

    public JalonLiaisonConsumer(IdempotentConsumer idempotent, ManageInstitutionUseCase institutions) {
        this.idempotent = idempotent;
        this.institutions = institutions;
    }

    @KafkaListener(
        id = GROUPE,
        topics = "${gimac.consommateurs.jalons.sujet:homologation.liaison}",
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
        String jalon = d.path("jalon").asString();
        if (!EN_PRODUCTION.equals(jalon)) {
            LOG.debug("Jalon {} de la liaison {} : sans effet sur l'institution", jalon, d.path("liaisonId").asString());
            return;
        }
        UUID institution = UUID.fromString(d.path("institutionId").asString());
        UUID liaison = UUID.fromString(d.path("liaisonId").asString());
        String reference = texte(d, "reference");
        String typeProjet = texte(d, "typeProjet");
        boolean traite = idempotent.handleOnce(e.path("id").asString(), GROUPE, () -> institutions.mettreEnProduction(institution, liaison, reference, typeProjet));
        if (!traite) {
            LOG.debug("Événement {} déjà traité", e.path("id").asString());
        }
    }

    private static String texte(JsonNode d, String champ) {
        return d.hasNonNull(champ) ? d.get(champ).asString() : null;
    }
}
