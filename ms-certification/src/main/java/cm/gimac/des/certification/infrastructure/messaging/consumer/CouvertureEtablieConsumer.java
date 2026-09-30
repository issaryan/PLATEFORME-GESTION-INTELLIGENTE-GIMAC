package cm.gimac.des.certification.infrastructure.messaging.consumer;

import cm.gimac.des.certification.application.usecase.ProcesVerbalService;
import cm.gimac.des.certification.infrastructure.messaging.Json;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

/**
 * Constat de la couverture de certification (homologation.coverage-established.v1, émis par MS-HOMOLOGATION
 * sur homologation.liaison ; homologation.coverage est aussi écouté, sujet d'agrégat de la règle générale) :
 * le procès-verbal devient établissable. Consommation idempotente (RG-15, RG-35) ; un message illisible part
 * en rebut par le gestionnaire d'erreurs commun.
 */
@Component
public class CouvertureEtablieConsumer {

    static final String TYPE = "homologation.coverage-established.v1";
    static final String GROUPE = "certification-couverture-etablie";
    private static final Logger LOG = LoggerFactory.getLogger(CouvertureEtablieConsumer.class);

    private final IdempotentConsumer idempotent;
    private final ProcesVerbalService procesVerbaux;

    public CouvertureEtablieConsumer(IdempotentConsumer idempotent, ProcesVerbalService procesVerbaux) {
        this.idempotent = idempotent;
        this.procesVerbaux = procesVerbaux;
    }

    @KafkaListener(
        id = GROUPE,
        topics = { "homologation.liaison", "homologation.coverage" },
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
        Integer taux = d.path("taux").isNumber() ? d.path("taux").asInt() : null;
        List<String> services = new ArrayList<>();
        d.path("servicesCouverts").forEach(s -> services.add(s.asString()));
        boolean traite = idempotent.handleOnce(e.path("id").asString(), GROUPE, () -> procesVerbaux.enregistrerCouvertureEtablie(liaison, taux, services));
        if (!traite) {
            LOG.debug("Événement {} déjà traité", e.path("id").asString());
        }
    }
}
