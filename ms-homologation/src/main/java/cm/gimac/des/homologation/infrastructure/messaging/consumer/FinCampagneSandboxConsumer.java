package cm.gimac.des.homologation.infrastructure.messaging.consumer;

import cm.gimac.des.homologation.application.usecase.sandbox.ReleveSandbox;
import cm.gimac.des.homologation.infrastructure.messaging.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * sbx-lecture.campaign-published.v1 (US-EXI-15, algorithme 6.53, CC-SBX-GIMAC-02 annexe D.7) : notification de fin de
 * campagne, de rectification d'un verdict après clôture ou d'annulation, publiée par SBX-LECTURE sur le sujet réservé
 * {@code sbx-lecture.campaign-published.v1} du bus de la plateforme (sujet nommé par le type, convention des relais de la sandbox) (écriture par SBX-LECTURE seul, lecture par le seul compte de
 * service de la plateforme du DES et par MS-AUDIT). Elle ne porte que l'identifiant de la campagne et un motif : elle
 * déclenche une relecture immédiate de la campagne par le contrat de lecture, sans jamais s'y substituer (la relève
 * périodique reste la source de vérité, et reprend une notification désactivée, perdue ou en échec).
 *
 * <p>Idempotence par identifiant d'événement (identifiant dérivé de la campagne, du motif et de l'événement déclencheur
 * par SBX-LECTURE) : une notification réémise à l'identique est acquittée sans second traitement. La relecture est
 * faite hors transaction englobante : chaque page relue est validée avec sa position, comme dans la relève périodique.
 * Une notification sans identifiant ou sans campagne est ignorée et journalisée.
 */
@Component
public class FinCampagneSandboxConsumer {

    static final String TYPE = "sbx-lecture.campaign-published.v1";
    static final String GROUPE = "homologation-fin-campagne-sandbox";
    private static final Logger LOG = LoggerFactory.getLogger(FinCampagneSandboxConsumer.class);

    private final ProcessedEventRepository traitees;
    private final IdempotentConsumer idempotent;
    private final ReleveSandbox releve;

    public FinCampagneSandboxConsumer(ProcessedEventRepository traitees, IdempotentConsumer idempotent, ReleveSandbox releve) {
        this.traitees = traitees;
        this.idempotent = idempotent;
        this.releve = releve;
    }

    @KafkaListener(
        id = GROUPE,
        topics = "${gimac.consommateurs.fin-campagne-sandbox.sujet:sbx-lecture.campaign-published.v1}",
        groupId = GROUPE,
        autoStartup = "${gimac.consommateurs.demarrage-automatique:true}"
    )
    public void recevoir(String message) {
        JsonNode e = Json.tree(message);
        String id = Json.texte(e, "id");
        if (!TYPE.equals(Json.texte(e, "type")) || id == null) {
            return;
        }
        String campagne = Json.texte(e.path("data"), "identifiantCampagne");
        if (campagne == null) {
            LOG.warn("Notification {} sans identifiant de campagne : ignorée", id);
            return;
        }
        if (traitees.existsByEventIdAndConsumerGroup(id, GROUPE)) {
            LOG.debug("Notification {} déjà traitée", id);
            return;
        }
        ReleveSandbox.Bilan bilan = releve.releverCampagneNotifiee(campagne);
        if (bilan.issue() != ReleveSandbox.Issue.REUSSIE) {
            // Supervision des notifications non traitées : la relève périodique reprendra la campagne.
            LOG.warn("Notification {} (campagne {}, motif {}) non traitée : relève périodique attendue", id, campagne, Json.texte(e.path("data"), "motif"));
        }
        idempotent.handleOnce(id, GROUPE, () -> {});
    }
}
