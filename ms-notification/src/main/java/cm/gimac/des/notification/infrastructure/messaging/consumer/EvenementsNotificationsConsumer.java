package cm.gimac.des.notification.infrastructure.messaging.consumer;

import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.DemandeDecidee;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.DemandeSoumise;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.EvenementProcessus;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.MessageEnvoye;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.RapportSoumis;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.TacheAffectee;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.TacheOfferte;
import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase.TacheRelancee;
import cm.gimac.des.notification.domain.service.ReglesNotifications.FaitsProcessus;
import cm.gimac.des.notification.domain.service.ReglesNotifications.NatureProcessus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Événements transformés en notifications (docs/lot1/notifications.md §1), enveloppes CloudEvents 1.0 :
 * collaboration.message-sent.v1 (sujet collaboration.message), pilotage.task-assigned.v1,
 * pilotage.task-offered.v1 et pilotage.task-reminded.v1 (sujet pilotage.task), pilotage.report-submitted.v1
 * (sujet pilotage.report), iam.request-submitted.v1 et iam.request-decided.v1 (sujet iam.request,
 * docs/lot1/pouvoirs-hierarchiques.md §4), et les six événements à destinataires des processus d'unité (sujet
 * pilotage.process, D-87, docs/lot1/processus-unite.md §5.5 : process-instance-started, process-step-activated,
 * process-step-submitted, process-step-signed, process-step-reassigned, process-instance-closed ; l'activation d'une
 * étape est aussi notifiée à l'initiateur de la demande, D-98) ; les autres
 * types de ces sujets (dont pilotage.process-published.v1) sont ignorés. Consommation idempotente par identifiant d'événement (table processed_event, RG-15,
 * RG-35) ; un message illisible part en rebut par le gestionnaire d'erreurs commun.
 */
@Component
public class EvenementsNotificationsConsumer {

    public static final String GROUPE = "notification-inapp";
    public static final String MESSAGE_ENVOYE = "collaboration.message-sent.v1";
    public static final String TACHE_AFFECTEE = "pilotage.task-assigned.v1";
    public static final String RAPPORT_SOUMIS = "pilotage.report-submitted.v1";
    public static final String TACHE_OFFERTE = "pilotage.task-offered.v1";
    public static final String TACHE_RELANCEE = "pilotage.task-reminded.v1";
    public static final String DEMANDE_SOUMISE = "iam.request-submitted.v1";
    public static final String DEMANDE_DECIDEE = "iam.request-decided.v1";
    public static final String INSTANCE_LANCEE = "pilotage.process-instance-started.v1";
    public static final String ETAPE_ACTIVEE = "pilotage.process-step-activated.v1";
    public static final String ETAPE_SOUMISE = "pilotage.process-step-submitted.v1";
    public static final String ETAPE_SIGNEE = "pilotage.process-step-signed.v1";
    public static final String ETAPE_REAFFECTEE = "pilotage.process-step-reassigned.v1";
    public static final String INSTANCE_CLOSE = "pilotage.process-instance-closed.v1";

    private static final Logger LOG = LoggerFactory.getLogger(EvenementsNotificationsConsumer.class);

    private final IdempotentConsumer idempotent;
    private final RecevoirEvenementsUseCase evenements;
    private final ObjectMapper json;

    public EvenementsNotificationsConsumer(IdempotentConsumer idempotent, RecevoirEvenementsUseCase evenements, ObjectMapper json) {
        this.idempotent = idempotent;
        this.evenements = evenements;
        this.json = json;
    }

    @KafkaListener(
        id = GROUPE,
        topics = "#{'${gimac.consommateurs.notification.sujets:collaboration.message,pilotage.task,pilotage.report,iam.request,pilotage.process}'.split(',')}",
        groupId = GROUPE,
        autoStartup = "${gimac.consommateurs.demarrage-automatique:true}"
    )
    @Transactional
    public void recevoir(String message) throws JsonProcessingException {
        JsonNode e = json.readTree(message);
        String type = texte(e, "type");
        String id = texte(e, "id");
        if (type == null || id == null) {
            LOG.debug("Enveloppe sans identifiant ou sans type : ignorée");
            return;
        }
        JsonNode d = e.path("data");
        if (d.isTextual()) {
            d = json.readTree(d.asText());
        }
        Runnable effet = effet(type, d);
        if (effet == null) {
            LOG.debug("Événement {} de type {} sans notification", id, type);
            return;
        }
        if (!idempotent.handleOnce(id, GROUPE, effet)) {
            LOG.debug("Événement {} déjà traité", id);
        }
    }

    private Runnable effet(String type, JsonNode d) {
        return switch (type) {
            case MESSAGE_ENVOYE -> () ->
                evenements.messageEnvoye(
                    new MessageEnvoye(
                        texte(d, "conversationId"),
                        texte(d, "messageId"),
                        texte(d, "auteurId"),
                        textes(d.path("destinataires")),
                        instant(d, "envoyeLe"),
                        texte(d, "evenement"),
                        // D-82 : champs facultatifs, absents des événements antérieurs.
                        texte(d, "reponseAAuteurId"),
                        textes(d.path("mentionnes"))
                    )
                );
            case TACHE_AFFECTEE -> () ->
                evenements.tacheAffectee(new TacheAffectee(texte(d, "tacheId"), texte(d, "responsableId"), texte(d, "acteurId"), instant(d, "le"), texte(d, "titre"), texte(d, "echeance")));
            case RAPPORT_SOUMIS -> () ->
                evenements.rapportSoumis(
                    new RapportSoumis(
                        texte(d, "rapportId"),
                        texte(d, "type"),
                        texte(d, "periode"),
                        texte(d, "auteurId"),
                        texte(d, "destinataireId"),
                        texte(d, "acteurId"),
                        instant(d, "le")
                    )
                );
            case TACHE_OFFERTE -> () ->
                evenements.tacheOfferte(
                    new TacheOfferte(
                        texte(d, "tacheId"),
                        texte(d, "titre"),
                        texte(d, "uniteCible"),
                        textes(d.path("destinataires")),
                        texte(d, "acteurId"),
                        instant(d, "le"),
                        texte(d, "lien")
                    )
                );
            case TACHE_RELANCEE -> () ->
                evenements.tacheRelancee(new TacheRelancee(texte(d, "tacheId"), texte(d, "titre"), texte(d, "responsableId"), texte(d, "acteurId"), instant(d, "le")));
            case DEMANDE_SOUMISE -> () ->
                evenements.demandeSoumise(
                    new DemandeSoumise(texte(d, "demandeId"), texte(d, "type"), texte(d, "demandeurId"), textes(d.path("destinataires")), instant(d, "le"))
                );
            case DEMANDE_DECIDEE -> () ->
                evenements.demandeDecidee(
                    new DemandeDecidee(texte(d, "demandeId"), texte(d, "type"), texte(d, "etat"), texte(d, "acteurId"), textes(d.path("destinataires")), instant(d, "le"))
                );
            case INSTANCE_LANCEE -> processus(d, NatureProcessus.INSTANCE_LANCEE, null, texte(d, "lanceurId"));
            case ETAPE_ACTIVEE -> avecInitiateur(d, processus(d, NatureProcessus.ETAPE_ACTIVEE, null, null));
            case ETAPE_SOUMISE -> processus(d, NatureProcessus.ETAPE_SOUMISE, texte(d, "attente"), texte(d, "acteurId"));
            case ETAPE_SIGNEE -> processus(d, NatureProcessus.ETAPE_SIGNEE, texte(d, "etatEtape"), texte(d, "signataireId"));
            case ETAPE_REAFFECTEE -> processus(d, NatureProcessus.ETAPE_REAFFECTEE, texte(d, "cause"), texte(d, "acteurId"));
            case INSTANCE_CLOSE -> processus(d, NatureProcessus.INSTANCE_CLOSE, texte(d, "etat"), texte(d, "acteurId"));
            default -> null;
        };
    }

    /**
     * Événement à destinataires d'un processus d'unité (D-87) : l'acteur (lanceur, exécutant, signataire, chef)
     * n'est jamais notifié ; une étape activée sans exécutant est à prendre par les porteurs du profil.
     */
    private Runnable processus(JsonNode d, NatureProcessus nature, String precision, String acteurId) {
        FaitsProcessus faits = new FaitsProcessus(
            nature,
            texte(d, "code"),
            texte(d, "intitule"),
            texte(d, "etape"),
            texte(d, "libelleEtape"),
            precision,
            d.path("renvoi").asBoolean(false),
            nature == NatureProcessus.ETAPE_ACTIVEE && texte(d, "executantId") == null,
            texte(d, "echeance")
        );
        // Personne remplacée : départ, retrait de profil ou changement d'unité (D-93 §9.3).
        String ancienId = nature == NatureProcessus.ETAPE_REAFFECTEE && precision != null && !"REAFFECTATION".equals(precision) ? texte(d, "ancienId") : null;
        return () ->
            evenements.evenementProcessus(
                new EvenementProcessus(texte(d, "instanceId"), faits, acteurId, ancienId, textes(d.path("destinataires")), texte(d, "lien"), instant(d, "le"))
            );
    }

    /**
     * Avancement d'une demande (D-98) : l'étape activée est aussi notifiée à l'initiateur ({@code lanceurId}, ajouté
     * à pilotage.process-step-activated.v1), sauf s'il en est lui-même destinataire (il agit) ou pour un renvoi (même
     * étape, déjà connue de lui). Un événement antérieur, sans {@code lanceurId}, ne notifie que ses destinataires.
     */
    private Runnable avecInitiateur(JsonNode d, Runnable etape) {
        String lanceur = texte(d, "lanceurId");
        if (lanceur == null || d.path("renvoi").asBoolean(false) || textes(d.path("destinataires")).contains(lanceur)) {
            return etape;
        }
        FaitsProcessus faits = new FaitsProcessus(
            NatureProcessus.DEMANDE_AVANCEE,
            texte(d, "code"),
            texte(d, "intitule"),
            texte(d, "etape"),
            texte(d, "libelleEtape"),
            null,
            false,
            texte(d, "executantId") == null,
            texte(d, "echeance")
        );
        EvenementProcessus demande = new EvenementProcessus(texte(d, "instanceId"), faits, null, null, List.of(lanceur), texte(d, "lien"), instant(d, "le"));
        return () -> {
            etape.run();
            evenements.evenementProcessus(demande);
        };
    }

    private static String texte(JsonNode n, String champ) {
        JsonNode v = n == null ? null : n.get(champ);
        return v == null || v.isNull() || v.asText().isBlank() ? null : v.asText();
    }

    private static List<String> textes(JsonNode n) {
        List<String> l = new ArrayList<>();
        if (n != null && n.isArray()) {
            n.forEach(x -> {
                if (x.isTextual() && !x.asText().isBlank()) {
                    l.add(x.asText());
                }
            });
        }
        return l;
    }

    /** Instant ISO-8601, ou secondes depuis l'époque (sérialisation numérique de Jackson) ; illisible : null. */
    private static Instant instant(JsonNode n, String champ) {
        JsonNode v = n == null ? null : n.get(champ);
        if (v == null || v.isNull()) {
            return null;
        }
        if (v.isNumber()) {
            return Instant.ofEpochSecond(v.longValue(), Math.round((v.doubleValue() - v.longValue()) * 1_000_000_000L));
        }
        try {
            return Instant.parse(v.asText());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
