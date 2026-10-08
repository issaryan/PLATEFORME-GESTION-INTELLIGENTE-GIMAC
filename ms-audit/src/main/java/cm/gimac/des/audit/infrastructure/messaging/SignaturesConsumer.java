package cm.gimac.des.audit.infrastructure.messaging;

import cm.gimac.des.audit.application.port.in.ScellerSignatureUseCase;
import cm.gimac.des.audit.application.port.in.ScellerSignatureUseCase.SignatureRecue;
import cm.gimac.des.audit.domain.registre.NatureSignature;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Signatures scellées au registre d'audit (D-93 §6 et §11), enveloppes CloudEvents 1.0 :
 * {@code pilotage.process-step-signed.v1} (sujet pilotage.process, processus d'unité D-87) et
 * {@code homologation.operation-signed.v1} (sujet homologation.operation, opérations D-91). Les autres types de ces
 * sujets sont ignorés. Idempotent : la table de déduplication (clé = identifiant CloudEvents) absorbe toute
 * relivraison ; un échec (base indisponible, enveloppe illisible) remonte, le message est rejoué puis mis en rebut,
 * jamais perdu en silence.
 */
@Component
public class SignaturesConsumer {

    public static final String GROUPE = "audit-signatures";

    private static final Logger LOG = LoggerFactory.getLogger(SignaturesConsumer.class);

    private final ScellerSignatureUseCase registre;
    private final ObjectMapper json;

    public SignaturesConsumer(ScellerSignatureUseCase registre, ObjectMapper json) {
        this.registre = registre;
        this.json = json;
    }

    @KafkaListener(
        id = GROUPE,
        topics = "#{'${gimac.consommateurs.signatures.sujets:pilotage.process,homologation.operation}'.split(',')}",
        groupId = GROUPE,
        autoStartup = "${gimac.consommateurs.demarrage-automatique:true}"
    )
    public void recevoir(String message) {
        JsonNode e = json.readTree(message);
        String id = texte(e, "id");
        Optional<NatureSignature> nature = NatureSignature.deType(texte(e, "type"));
        if (id == null || nature.isEmpty()) {
            LOG.debug("Événement sans identifiant ou hors signatures : ignoré");
            return;
        }
        JsonNode d = e.path("data");
        if (d.isString()) {
            d = json.readTree(d.asString());
        }
        Instant survenue = instant(texte(e, "time"));
        if (survenue == null) {
            survenue = instant(texte(d, "le"));
        }
        SignatureRecue s = new SignatureRecue(id, nature.orElseThrow(), survenue, texte(e, "correlationid"), texte(d, "signataireId"), message);
        if (registre.sceller(s).isEmpty()) {
            LOG.debug("Signature {} déjà scellée", id);
        }
    }

    private static String texte(JsonNode n, String champ) {
        JsonNode v = n == null ? null : n.get(champ);
        if (v == null || v.isNull() || v.isContainer()) {
            return null;
        }
        String s = v.asString();
        return s == null || s.isBlank() ? null : s;
    }

    private static Instant instant(String s) {
        if (s == null) {
            return null;
        }
        try {
            return Instant.parse(s);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
