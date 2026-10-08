package cm.gimac.des.homologation.infrastructure.messaging.consumer;

import cm.gimac.des.homologation.application.usecase.EvenementsCertification;
import cm.gimac.des.homologation.application.usecase.EvenementsCertification.Signature;
import cm.gimac.des.homologation.domain.parcours.TypeAttestation;
import cm.gimac.des.homologation.infrastructure.messaging.Json;
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
 * Événements de MS-CERTIFICATION (docs/lot1/parcours-backend.md §3), sujets certification.proces-verbal et
 * certification.approbation : procès-verbal établi et signé (jalon CERTIFIEE), dossier de changement, fiche de
 * changement référencée, attestations archivées, sécurité approuvée (jalon APPROUVEE_SECURITE) ; chaque sujet
 * clôt les opérations du référentiel publié qui le portent comme déclencheur (D-77). Événements facultatifs tolérés pour la
 * projection des compteurs : attestation déposée ou vérifiée, visas attendus ou signés. Consommation
 * idempotente (RG-15, RG-35) ; un message illisible part en rebut par le gestionnaire d'erreurs commun.
 */
@Component
public class EvenementsCertificationConsumer {

    static final String GROUPE = "homologation-certification";
    private static final Logger LOG = LoggerFactory.getLogger(EvenementsCertificationConsumer.class);

    private final IdempotentConsumer idempotent;
    private final EvenementsCertification effets;

    public EvenementsCertificationConsumer(IdempotentConsumer idempotent, EvenementsCertification effets) {
        this.idempotent = idempotent;
        this.effets = effets;
    }

    @KafkaListener(
        id = GROUPE,
        topics = "#{'${gimac.consommateurs.certification.sujets:certification.proces-verbal,certification.approbation}'.split(',')}",
        groupId = GROUPE,
        autoStartup = "${gimac.consommateurs.demarrage-automatique:true}"
    )
    @Transactional
    public void recevoir(String message) {
        JsonNode e = Json.tree(message);
        String type = e.path("type").asString();
        String id = e.path("id").asString();
        if (type == null || !type.startsWith("certification.") || id == null || id.isBlank()) {
            return;
        }
        JsonNode d = e.path("data");
        String liaison = d.path("liaisonId").asString();
        if (liaison == null || liaison.isBlank()) {
            LOG.warn("Événement {} ({}) sans liaison : ignoré", id, type);
            return;
        }
        UUID l = UUID.fromString(liaison);
        Runnable effet = effet(type, l, d);
        if (effet == null) {
            LOG.debug("Événement {} de type {} sans effet sur le parcours", id, type);
            return;
        }
        if (!idempotent.handleOnce(id, GROUPE, effet)) {
            LOG.debug("Événement {} déjà traité", id);
        }
    }

    private Runnable effet(String type, UUID l, JsonNode d) {
        return switch (type) {
            case "certification.pv-established.v1" -> () -> effets.pvEtabli(l, texte(d, "etabliPar"), texte(d, "empreinte"));
            case "certification.pv-signed.v1" -> () -> effets.pvSigne(l, signatures(d.path("signatures")), texte(d, "empreinte"));
            case "certification.change-record-constituted.v1" -> () -> effets.dossierChangementConstitue(l, texte(d, "documentRef"), texte(d, "nom"), texte(d, "empreinte"));
            case "certification.change-record-referenced.v1" -> () -> effets.ficheChangementReferencee(l, texte(d, "reference"));
            case "certification.attestations-archived.v1" -> () -> effets.attestationsArchivees(l, references(d.path("attestations")));
            case "certification.security-approved.v1" -> () -> effets.securiteApprouvee(l, texte(d, "approuveeLe"));
            // Nom émis par MS-CERTIFICATION (attestation-submitted) ; « deposited » reste admis.
            case "certification.attestation-submitted.v1", "certification.attestation-deposited.v1" -> () -> effets.attestationDeposee(l, TypeAttestation.valueOf(texte(d, "type")));
            case "certification.attestation-verified.v1" -> () -> effets.attestationVerifiee(l, TypeAttestation.valueOf(texte(d, "type")));
            case "certification.visas-required.v1" -> () -> effets.visasAttendus(l, textes(d.path("natures")));
            case "certification.visa-signed.v1" -> () -> effets.visaSigne(l, texte(d, "nature"));
            default -> null;
        };
    }

    private static String texte(JsonNode d, String champ) {
        JsonNode n = d.path(champ);
        return n.isMissingNode() || n.isNull() ? null : n.asString();
    }

    private static List<Signature> signatures(JsonNode n) {
        List<Signature> l = new ArrayList<>();
        n.forEach(s -> l.add(new Signature(texte(s, "fonction"), texte(s, "signataire"), texte(s, "signeLe"), texte(s, "signature"))));
        return l;
    }

    private static List<String> references(JsonNode n) {
        List<String> l = new ArrayList<>();
        n.forEach(a -> {
            String r = texte(a, "documentRef");
            if (r != null) {
                l.add(r);
            }
        });
        return l;
    }

    private static List<String> textes(JsonNode n) {
        List<String> l = new ArrayList<>();
        n.forEach(x -> l.add(x.asString()));
        return l;
    }
}
