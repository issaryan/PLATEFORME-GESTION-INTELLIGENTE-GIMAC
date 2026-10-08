package cm.gimac.des.certification.infrastructure.messaging.consumer;

import cm.gimac.des.certification.application.usecase.OperationsLiaison;
import cm.gimac.des.certification.application.usecase.OperationsLiaison.OperationRecue;
import cm.gimac.des.certification.domain.decision.EtapeSignature;
import cm.gimac.des.certification.domain.operation.Intervenant;
import cm.gimac.des.certification.domain.operation.OperationLiaison;
import cm.gimac.des.certification.infrastructure.messaging.Json;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

/**
 * Opérations du parcours de MS-HOMOLOGATION (D-93 §4, §8.2) : projection locale qui fonde le contrôle des gestes de
 * certification. Sujet homologation.operation : operation-activated.v1 (ouverte), operation-validation-requested.v1
 * (en validation), operation-signed.v1 (en signature ou faite), operation-closed.v1 (faite ou sans objet, clôtures
 * forcées comprises) ; le déclencheur et les signatures configurées de l'opération sont lus dans ces événements ; sujet
 * homologation.liaison :
 * liaison-milestone-reached.v1 (opération du jalon faite). Les autres types sont ignorés. Consommation idempotente
 * (RG-15, RG-35) ; un message illisible part en rebut par le gestionnaire d'erreurs commun.
 */
@Component
public class OperationsHomologationConsumer {

    static final String GROUPE = "certification-operations";
    static final String ACTIVEE = "homologation.operation-activated.v1";
    static final String VALIDATION = "homologation.operation-validation-requested.v1";
    static final String SIGNEE = "homologation.operation-signed.v1";
    static final String CLOSE = "homologation.operation-closed.v1";
    static final String JALON = "homologation.liaison-milestone-reached.v1";
    static final Set<String> TYPES = Set.of(ACTIVEE, VALIDATION, SIGNEE, CLOSE, JALON);
    private static final Logger LOG = LoggerFactory.getLogger(OperationsHomologationConsumer.class);

    private final IdempotentConsumer idempotent;
    private final OperationsLiaison operations;

    public OperationsHomologationConsumer(IdempotentConsumer idempotent, OperationsLiaison operations) {
        this.idempotent = idempotent;
        this.operations = operations;
    }

    @KafkaListener(
        id = GROUPE,
        topics = { "homologation.operation", "homologation.liaison" },
        groupId = GROUPE,
        autoStartup = "${gimac.consommateurs.demarrage-automatique:true}"
    )
    @Transactional
    public void recevoir(String message) {
        JsonNode e = Json.tree(message);
        String type = e.path("type").asString();
        if (!TYPES.contains(type)) {
            return;
        }
        JsonNode d = e.path("data");
        UUID liaison = UUID.fromString(d.path("liaisonId").asString());
        UUID operation = d.hasNonNull("operationId") ? UUID.fromString(d.path("operationId").asString()) : null;
        if (operation == null) {
            LOG.warn("Événement {} sans opération pour la liaison {} : ignoré", type, liaison);
            return;
        }
        Instant le = instant(d, e);
        Runnable effet = switch (type) {
            case ACTIVEE -> () -> operations.activee(operation(d, liaison, operation, le));
            case VALIDATION -> () -> operations.enValidation(operation(d, liaison, operation, le));
            case CLOSE -> () ->
                operations.cloturee(
                    liaison,
                    operation,
                    texte(d, "etat"),
                    new OperationLiaison.Description(texte(d, "code"), texte(d, "libelle"), null, null, texte(d, "jalon"), texte(d, "declencheur")),
                    le
                );
            case SIGNEE -> () -> operations.signee(liaison, operation, texte(d, "rang"), texte(d, "libelle"), d.path("operationClose").asBoolean(), le);
            default -> () -> operations.jalonAtteint(liaison, operation, texte(d, "code"), texte(d, "libelle"), texte(d, "jalon"), le);
        };
        if (!idempotent.handleOnce(e.path("id").asString(), GROUPE, effet)) {
            LOG.debug("Événement {} déjà traité", e.path("id").asString());
        }
    }

    static OperationRecue operation(JsonNode d, UUID liaison, UUID operation, Instant le) {
        List<Intervenant> intervenants = new ArrayList<>();
        intervenant(d.path("responsable"), "R", intervenants);
        intervenant(d.path("validateur"), "V", intervenants);
        d.path("destinataires").forEach(x -> intervenant(x, null, intervenants));
        List<String> institutions = new ArrayList<>();
        d.path("institutionsConcernees").forEach(x -> institutions.add(x.asString()));
        if (institutions.isEmpty() && d.hasNonNull("institutionId")) {
            institutions.add(d.path("institutionId").asString());
        }
        List<EtapeSignature> signatures = null;
        if (d.path("signatures").isArray()) {
            signatures = new ArrayList<>();
            for (JsonNode s : d.path("signatures")) {
                signatures.add(
                    new EtapeSignature(texte(s, "code"), texte(s, "libelle"), texte(s, "signataire"), s.path("ordre").isNumber() ? s.path("ordre").asInt() : 1, false)
                );
            }
        }
        return new OperationRecue(
            liaison,
            operation,
            le,
            new OperationLiaison.Description(
                texte(d, "code"),
                texte(d, "libelle"),
                texte(d, "geste"),
                texte(d, "vue"),
                texte(d, "jalon"),
                texte(d, "declencheur")
            ),
            intervenants,
            institutions,
            signatures
        );
    }

    private static void intervenant(JsonNode x, String qualite, List<Intervenant> cible) {
        if (x == null || !x.isObject()) {
            return;
        }
        String q = texte(x, "qualite");
        cible.add(
            new Intervenant(
                q != null ? q : qualite,
                texte(x, "fonction"),
                texte(x, "compteId"),
                texte(x, "institutionId"),
                x.path("aAffecter").asBoolean()
            )
        );
    }

    private static Instant instant(JsonNode d, JsonNode e) {
        String le = d.hasNonNull("le") ? d.path("le").asString() : e.hasNonNull("time") ? e.path("time").asString() : null;
        return le == null ? Instant.EPOCH : Instant.parse(le);
    }

    private static String texte(JsonNode d, String champ) {
        return d.hasNonNull(champ) ? d.get(champ).asString() : null;
    }
}
