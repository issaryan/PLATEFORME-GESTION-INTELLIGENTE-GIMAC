package cm.gimac.des.audit.infrastructure.messaging;

import cm.gimac.des.audit.application.port.in.InscrireEvenementSandboxUseCase;
import cm.gimac.des.audit.domain.sandbox.EnveloppeSandbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Événements à effet des onze services de la Sandbox GIMAC (US-EXI-10, AUD-01) : abonnement par motif aux sujets de la
 * sandbox, nommés par le type ({@code sbx-espace.space-opened.v1}) ou par l'agrégat (files de rebut exclues), enveloppe CloudEvents validée ({@link EnveloppeSandbox}),
 * inscription idempotente au registre (domaine {@code sandbox}, institution retenue). Une enveloppe non conforme va au
 * rebut avec son motif (AUD-07), sans jamais être inscrite ; les demandes de notification sont ignorées (MS-NOTIFICATION).
 * Une panne de la base remonte : le message est rejoué, jamais perdu en silence.
 */
@Component
public class EvenementsSandboxConsumer {

    public static final String GROUPE = "audit-sandbox";

    private static final Logger LOG = LoggerFactory.getLogger(EvenementsSandboxConsumer.class);

    private final InscrireEvenementSandboxUseCase registre;
    private final ObjectMapper json;

    public EvenementsSandboxConsumer(InscrireEvenementSandboxUseCase registre, ObjectMapper json) {
        this.registre = registre;
        this.json = json;
    }

    @KafkaListener(
        id = GROUPE,
        topicPattern = "${gimac.consommateurs.sandbox.motif:sbx-[a-z0-9]+\\.[a-z0-9-]+(\\.v[0-9]+)?}",
        groupId = GROUPE,
        autoStartup = "${gimac.consommateurs.demarrage-automatique:true}"
    )
    public void recevoir(@Payload(required = false) String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String sujet) {
        // Valeur convertie par Spring (octets ou texte selon le désérialiseur du profil), sujet tiré de l'en-tête.
        traiter(sujet, message);
    }

    /** Lecture, validation et inscription d'un message reçu sur {@code sujet}. */
    public void traiter(String sujet, String message) {
        JsonNode e;
        try {
            e = message == null ? null : json.readTree(message);
        } catch (JacksonException ex) {
            e = null;
        }
        if (e == null || !e.isObject()) {
            registre.rebuter(null, sujet, "JSON_ILLISIBLE", message);
            return;
        }
        JsonNode d = e.path("data");
        EnveloppeSandbox.Lecture l = EnveloppeSandbox.lire(
            new EnveloppeSandbox.Champs(
                texte(e, "specversion"),
                texte(e, "id"),
                texte(e, "type"),
                texte(e, "source"),
                texte(e, "time"),
                d.isObject(),
                texte(e, "institutionid"),
                premier(d, "codeParticipant", "institutionId", "institution"),
                premier(d, "acteurId", "auteurId", "sub"),
                texte(e, "correlationid")
            )
        );
        if (l.ignore()) {
            LOG.debug("Demande de notification {} : sans effet à inscrire", texte(e, "id"));
            return;
        }
        if (l.motif() != null) {
            registre.rebuter(texte(e, "id"), sujet, l.motif(), message);
            return;
        }
        if (registre.inscrire(l.enveloppe(), message).isEmpty()) {
            LOG.debug("Événement {} de la sandbox déjà inscrit", l.enveloppe().id());
        }
    }

    private static String premier(JsonNode n, String... champs) {
        for (String c : champs) {
            String v = texte(n, c);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    /** Champ textuel de {@code n} (enveloppe, ou ses données lues par path, jamais null) ; absent, nul, composé ou blanc : null. */
    private static String texte(JsonNode n, String champ) {
        JsonNode v = n.get(champ);
        if (v == null || v.isNull() || v.isContainer()) {
            return null;
        }
        String s = v.asString();
        return s.isBlank() ? null : s;
    }
}
