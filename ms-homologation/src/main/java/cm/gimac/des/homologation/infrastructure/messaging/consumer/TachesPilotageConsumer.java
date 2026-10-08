package cm.gimac.des.homologation.infrastructure.messaging.consumer;

import cm.gimac.des.homologation.application.usecase.DemandesTaches;
import cm.gimac.des.homologation.application.usecase.PrisesFonctions;
import cm.gimac.des.homologation.infrastructure.messaging.Json;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

/**
 * Prise d'une tâche à prendre (D-93, D-94, docs/lot1/effet-reel-referentiels.md §8.8) : sujet pilotage.task de
 * MS-PILOTAGE, type pilotage.task-taken.v1 (charge {@code { tacheId, origine: { type, reference, service, lien },
 * responsableId, acteurId, mode, profilsRequis, permissionsRequises, … }}). Seules les tâches de ce service
 * ({@code origine.service} = homologation) dont la référence finit par {@code /AFFECTER/<FONCTION>} sont traitées : la
 * liaison est lue dans le lien ({@code /des/parcours/<liaison>}), la personne est affectée à la fonction. Consommation
 * idempotente par l'identifiant CloudEvents ; un autre type ou une autre origine est ignoré.
 */
@Component
public class TachesPilotageConsumer {

    static final String GROUPE = "homologation-taches";
    static final String PRISE = "pilotage.task-taken.v1";
    private static final Pattern LIEN = Pattern.compile("^/des/parcours/([0-9a-fA-F-]{36})(?:[?#/].*)?$");
    private static final Logger LOG = LoggerFactory.getLogger(TachesPilotageConsumer.class);

    private final IdempotentConsumer idempotent;
    private final PrisesFonctions prises;

    public TachesPilotageConsumer(IdempotentConsumer idempotent, PrisesFonctions prises) {
        this.idempotent = idempotent;
        this.prises = prises;
    }

    @KafkaListener(
        id = GROUPE,
        topics = "${gimac.consommateurs.taches.sujet:pilotage.task}",
        groupId = GROUPE,
        autoStartup = "${gimac.consommateurs.demarrage-automatique:true}"
    )
    @Transactional
    public void recevoir(String message) {
        JsonNode e = Json.tree(message);
        String type = e.path("type").asString();
        String id = e.path("id").asString();
        JsonNode d = e.path("data");
        JsonNode o = d.path("origine");
        String service = texte(o.path("service"));
        String reference = texte(o.path("reference"));
        String responsable = texte(d.path("responsableId"));
        UUID liaison = liaison(texte(o.path("lien")));
        if (!PRISE.equals(type) || id == null || id.isBlank() || !DemandesTaches.SERVICE.equals(service) || reference == null || responsable == null || liaison == null) {
            return;
        }
        String acteur = texte(d.path("acteurId"));
        String mode = texte(d.path("mode"));
        if (!idempotent.handleOnce(id, GROUPE, () -> prises.affecter(liaison, reference, responsable, acteur, mode))) {
            LOG.debug("Événement {} déjà traité", id);
        }
    }

    /** Liaison du lien d'origine {@code /des/parcours/<liaison>?…}, sinon null. */
    static UUID liaison(String lien) {
        if (lien == null) {
            return null;
        }
        Matcher m = LIEN.matcher(lien);
        if (!m.matches()) {
            return null;
        }
        try {
            return UUID.fromString(m.group(1));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String texte(JsonNode n) {
        if (n == null || n.isMissingNode() || n.isNull()) {
            return null;
        }
        String s = n.asString();
        return s == null || s.isBlank() ? null : s;
    }
}
