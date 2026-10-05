package cm.gimac.des.homologation.application.usecase;

import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.Preuve;
import cm.gimac.des.homologation.domain.parcours.TypeAttestation;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Effets des événements de MS-CERTIFICATION sur le parcours (docs/lot1/parcours-backend.md §3) : clôture des
 * opérations dont le déclencheur est le sujet reçu (D-77 : lu sur l'opération, aucun rang codé en dur), jalons
 * CERTIFIEE et APPROUVEE_SECURITE, projection de l'état des verrous lue par les
 * tableaux de bord et les tâches du portail. Appelé par le consommateur idempotent ; une liaison inconnue est ignorée.
 */
@Service
@Transactional
public class EvenementsCertification {

    private static final Logger LOG = LoggerFactory.getLogger(EvenementsCertification.class);

    /** Signature portée par certification.pv-signed.v1. */
    public record Signature(String fonction, String signataire) {}

    private final ParcoursDepot depot;
    private final Clock clock;

    public EvenementsCertification(ParcoursDepot depot, Clock clock) {
        this.depot = depot;
        this.clock = clock;
    }

    private Optional<Parcours> parcours(UUID liaisonId) {
        Optional<Parcours> p = depot.trouver(liaisonId, true);
        if (p.isEmpty()) {
            LOG.warn("Événement de certification pour une liaison sans parcours : {}", liaisonId);
        }
        return p;
    }

    private LocalDate aujourdhui() {
        return LocalDate.now(clock);
    }

    private static String ref(Parcours p) {
        return p.etat().getReference();
    }

    public void pvEtabli(UUID liaisonId, String etabliPar, String empreinte) {
        parcours(liaisonId).ifPresent(p ->
            p.pvEtabli(Preuve.piece("PV-" + ref(p), "proces-verbal-certification-" + ref(p) + ".pdf", empreinte, aujourdhui(), etabliPar), aujourdhui())
        );
    }

    public void pvSigne(UUID liaisonId, List<Signature> signatures, String empreinte) {
        parcours(liaisonId).ifPresent(p -> {
            boolean participant = signatures.stream().anyMatch(s -> "PARTICIPANT".equals(s.fonction()));
            boolean gimac = signatures.stream().anyMatch(s -> s.fonction() != null && !"PARTICIPANT".equals(s.fonction()));
            String par = signatures.stream().map(Signature::signataire).filter(s -> s != null && !s.isBlank()).reduce((a, b) -> a + ", " + b).orElse("");
            p.pvSigne(gimac, participant, Preuve.piece("PV-SIG-" + ref(p), "proces-verbal-signe.pdf", empreinte, aujourdhui(), par), aujourdhui());
        });
    }

    public void dossierChangementConstitue(UUID liaisonId, String documentRef, String nom, String empreinte) {
        parcours(liaisonId).ifPresent(p -> p.dossierChangementConstitue(Preuve.piece(documentRef, nom, empreinte, aujourdhui(), "PRODUCTION_MONETIQUE"), aujourdhui()));
    }

    public void ficheChangementReferencee(UUID liaisonId, String reference) {
        parcours(liaisonId).ifPresent(p ->
            p.ficheChangementReferencee(reference, Preuve.piece(reference, reference, ParcoursCommandes.sha256(reference), aujourdhui(), "PRODUCTION_MONETIQUE"), aujourdhui())
        );
    }

    public void attestationsArchivees(UUID liaisonId, List<String> documentRefs) {
        parcours(liaisonId).ifPresent(p -> {
            String premier = documentRefs.isEmpty() ? "ATT-" + ref(p) : documentRefs.get(0);
            p.attestationsArchivees(Preuve.piece(premier, "attestations-" + ref(p), ParcoursCommandes.sha256(String.join(",", documentRefs)), aujourdhui(), "RSSI"), aujourdhui());
        });
    }

    public void securiteApprouvee(UUID liaisonId, String approuveeLe) {
        parcours(liaisonId).ifPresent(p ->
            p.securiteApprouvee(Preuve.piece("SSI-" + ref(p), "approbation-securite-" + ref(p), ParcoursCommandes.sha256(liaisonId + ":" + approuveeLe), aujourdhui(), "RSSI"), aujourdhui())
        );
    }

    public void attestationDeposee(UUID liaisonId, TypeAttestation type) {
        parcours(liaisonId).ifPresent(p -> p.attestationDeposee(type));
    }

    public void attestationVerifiee(UUID liaisonId, TypeAttestation type) {
        parcours(liaisonId).ifPresent(p -> p.attestationVerifiee(type));
    }

    public void visasAttendus(UUID liaisonId, List<String> natures) {
        parcours(liaisonId).ifPresent(p -> p.visas(natures));
    }

    public void visaSigne(UUID liaisonId, String nature) {
        parcours(liaisonId).ifPresent(p -> p.visaSigne(nature));
    }
}
