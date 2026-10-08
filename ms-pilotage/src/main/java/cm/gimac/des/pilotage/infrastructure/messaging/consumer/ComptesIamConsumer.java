package cm.gimac.des.pilotage.infrastructure.messaging.consumer;

import cm.gimac.des.pilotage.application.port.in.InstancesProcessusUseCase;
import cm.gimac.des.pilotage.domain.ProcessedEvent;
import cm.gimac.des.pilotage.repository.ProcessedEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Changements de MS-IAM qui touchent les intervenants des processus d'unité (D-87 §2, D-93 §6).
 *
 * <p>Départ ou désactivation d'un compte : sujet iam.account de MS-IAM, type
 * iam.account-administered.v1 (charge {@code { action, compteId, identifiant, acteurId, motif, details }}) avec
 * l'action DESACTIVATION ou SUPPRESSION (clé et subject : identifiant du compte). Les étapes en cours du compte, ses signatures et validations attendues
 * passent au profil de remplacement. Consommation idempotente par l'identifiant CloudEvents (table
 * processed_event) ; un message illisible est journalisé et ignoré ; une erreur de traitement (MS-IAM
 * injoignable) remonte pour que Kafka le rejoue.
 *
 * <p>Même sujet, même type (D-93) : action PROFILS (profils du compte remplacés : retrait de profil) et
 * COMPTE_RATTACHEMENT_MODIFIE (unité ou responsable changés) : les rôles que le compte tient sur des étapes en cours
 * sont revus. Sujet iam.configuration ({@code gimac.consommateurs.configuration.sujet}), type
 * iam.configuration-changed.v1, objet UNITE_ORGANISATIONNELLE (champs {@code code=…}, {@code responsable=…}) : les
 * rôles CHEF_UNITE de l'unité passent à son chef actuel (groupe pilotage-processus-organisation).
 */
@Component
public class ComptesIamConsumer {

    static final String GROUPE = "pilotage-processus-comptes";
    static final String ADMINISTRE = "iam.account-administered.v1";
    static final Set<String> DEPARTS = Set.of("DESACTIVATION", "SUPPRESSION");
    static final String PROFILS = "PROFILS";
    static final String RATTACHEMENT = "COMPTE_RATTACHEMENT_MODIFIE";
    static final String GROUPE_ORGANISATION = "pilotage-processus-organisation";
    static final String CONFIGURATION = "iam.configuration-changed.v1";
    static final String OBJET_UNITE = "UNITE_ORGANISATIONNELLE";
    private static final Logger LOG = LoggerFactory.getLogger(ComptesIamConsumer.class);

    private final InstancesProcessusUseCase processus;
    private final ProcessedEventRepository traites;
    private final ObjectMapper json;
    private final Clock horloge;

    public ComptesIamConsumer(InstancesProcessusUseCase processus, ProcessedEventRepository traites, ObjectMapper json, Clock horloge) {
        this.processus = processus;
        this.traites = traites;
        this.json = json;
        this.horloge = horloge;
    }

    @KafkaListener(
        id = GROUPE,
        topics = "${gimac.consommateurs.comptes.sujet:iam.account}",
        groupId = GROUPE,
        autoStartup = "${gimac.consommateurs.demarrage-automatique:true}",
        properties = { "auto.offset.reset=earliest" }
    )
    @Transactional
    public void recevoir(String message) {
        JsonNode e;
        try {
            e = json.readTree(message);
        } catch (JsonProcessingException ex) {
            LOG.warn("Message illisible sur le sujet des comptes : ignoré");
            return;
        }
        String type = e.path("type").asText("");
        JsonNode d = e.path("data");
        Optional<String> compte = depart(type, d);
        Optional<String> change = changement(type, d);
        if (compte.isEmpty() && change.isEmpty()) {
            return;
        }
        Optional<UUID> id = uuid(e.path("id").asText(null));
        if (id.isPresent() && traites.existsByEventId(id.orElseThrow())) {
            LOG.debug("Événement {} déjà traité", id.orElseThrow());
            return;
        }
        if (compte.isPresent()) {
            int n = processus.traiterDepart(compte.orElseThrow(), texte(d, "acteurId"));
            LOG.info("Départ d'un compte : {} étape(s) de processus réaffectée(s)", n);
        } else {
            String cause = PROFILS.equals(texte(d, "action")) ? "PROFIL_RETIRE" : "CHANGEMENT_UNITE";
            int n = processus.traiterChangementCompte(change.orElseThrow(), cause, texte(d, "acteurId"));
            LOG.info("Compte modifié ({}) : {} étape(s) de processus revue(s)", cause, n);
        }
        id.ifPresent(x -> traites.save(new ProcessedEvent().eventId(x).subject(type).processedAt(horloge.instant())));
    }

    /** Unité modifiée (iam.configuration-changed.v1, objet UNITE_ORGANISATIONNELLE) : rôles CHEF_UNITE revus. */
    @KafkaListener(
        id = GROUPE_ORGANISATION,
        topics = "${gimac.consommateurs.configuration.sujet:iam.configuration}",
        groupId = GROUPE_ORGANISATION,
        autoStartup = "${gimac.consommateurs.demarrage-automatique:true}",
        properties = { "auto.offset.reset=earliest" }
    )
    @Transactional
    public void recevoirConfiguration(String message) {
        JsonNode e;
        try {
            e = json.readTree(message);
        } catch (JsonProcessingException ex) {
            LOG.warn("Message illisible sur le sujet de configuration : ignoré");
            return;
        }
        String type = e.path("type").asText("");
        Optional<String> unite = uniteModifiee(type, e.path("data"));
        if (unite.isEmpty()) {
            return;
        }
        Optional<UUID> id = uuid(e.path("id").asText(null));
        if (id.isPresent() && traites.existsByEventId(id.orElseThrow())) {
            LOG.debug("Événement {} déjà traité", id.orElseThrow());
            return;
        }
        int n = processus.traiterChangementChef(unite.orElseThrow(), texte(e.path("data"), "acteurId"));
        id.ifPresent(x -> traites.save(new ProcessedEvent().eventId(x).subject(type).processedAt(horloge.instant())));
        LOG.info("Unité modifiée : {} étape(s) de processus revue(s)", n);
    }

    /** Profils remplacés ou rattachement modifié : compte dont les rôles sont à revoir. */
    static Optional<String> changement(String type, JsonNode d) {
        String compte = texte(d, "compteId");
        String action = texte(d, "action");
        if (compte != null && ADMINISTRE.equals(type) && (PROFILS.equals(action) || RATTACHEMENT.equals(action))) {
            return Optional.of(compte);
        }
        return Optional.empty();
    }

    /** Code de l'unité modifiée, lu dans les champs {@code code=…} d'un changement de configuration d'unité. */
    static Optional<String> uniteModifiee(String type, JsonNode d) {
        if (!CONFIGURATION.equals(type) || !OBJET_UNITE.equals(texte(d, "objet"))) {
            return Optional.empty();
        }
        for (JsonNode c : d.path("champs")) {
            String v = c.asText("");
            if (v.startsWith("code=") && v.length() > "code=".length()) {
                return Optional.of(v.substring("code=".length()));
            }
        }
        return Optional.empty();
    }

    /** Compte parti : désactivé ou supprimé par l'administration. */
    static Optional<String> depart(String type, JsonNode d) {
        String compte = texte(d, "compteId");
        if (compte == null) {
            return Optional.empty();
        }
        if (ADMINISTRE.equals(type) && DEPARTS.contains(String.valueOf(texte(d, "action")))) {
            return Optional.of(compte);
        }
        return Optional.empty();
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
