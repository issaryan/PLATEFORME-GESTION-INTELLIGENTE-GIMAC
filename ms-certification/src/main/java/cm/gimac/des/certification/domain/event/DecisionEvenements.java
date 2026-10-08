package cm.gimac.des.certification.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Charges utiles des événements des verrous de la décision (docs/lot1/parcours-backend.md §3), émis par
 * l'outbox en CloudEvents 1.0 avec l'extension institutionid. Aucun ne porte de secret ni de contenu de
 * pièce : références, empreintes, fonctions et instants seulement. Sujets : certification.proces-verbal
 * (verrou 1) et certification.approbation (verrou 2).
 */
public final class DecisionEvenements {

    public static final String SUJET_PROCES_VERBAL = "certification.proces-verbal";
    public static final String SUJET_APPROBATION = "certification.approbation";

    public static final String PV_ETABLI = "certification.pv-established.v1";
    public static final String PV_SIGNE = "certification.pv-signed.v1";
    /** Complément au §3 : signature d'une des deux parties (projection pvASigner de MS-HOMOLOGATION). */
    public static final String PV_SIGNATURE_ENREGISTREE = "certification.pv-signature-recorded.v1";
    public static final String DOSSIER_CHANGEMENT_CONSTITUE = "certification.change-record-constituted.v1";
    public static final String FICHE_CHANGEMENT_REFERENCEE = "certification.change-record-referenced.v1";
    /** Complément au §3 : attestation déposée au portail (projection attestationsAVerifier). */
    public static final String ATTESTATION_DEPOSEE = "certification.attestation-submitted.v1";
    /** Complément au §3 : attestation vérifiée conforme ou rejetée avec motif (projection attestationsAFournir). */
    public static final String ATTESTATION_VERIFIEE = "certification.attestation-verified.v1";
    public static final String ATTESTATIONS_ARCHIVEES = "certification.attestations-archived.v1";
    /** Complément au §3 : visas optionnels activés à l'ouverture du verrou 2 (projection visasAttendus). */
    public static final String VISAS_REQUIS = "certification.visas-required.v1";
    /** Complément au §3 : visa optionnel signé (projection visasAttendus). */
    public static final String VISA_SIGNE = "certification.visa-signed.v1";
    public static final String SECURITE_APPROUVEE = "certification.security-approved.v1";

    private DecisionEvenements() {}

    public record ProcesVerbalEtabli(UUID liaisonId, String etabliPar, String etabliParId, String empreinte) {}

    /** Signature du procès-verbal : fonction signataire, nom, instant et code de la signature du circuit (D-94). */
    public record SignatureResume(String fonction, String signataire, Instant signeLe, String signature) {}

    public record ProcesVerbalSigne(UUID liaisonId, List<SignatureResume> signatures, String empreinte) {}

    /** Signature enregistrée : fonction, instant, état du procès-verbal qui en résulte et code de la signature (D-94). */
    public record SignatureProcesVerbalEnregistree(UUID liaisonId, String fonction, Instant signeLe, String etat, String signature) {}

    public record DossierChangementConstitue(UUID liaisonId, String documentRef, String nom, String empreinte) {}

    public record FicheChangementReferencee(UUID liaisonId, String reference) {}

    public record AttestationDeposee(UUID liaisonId, String type, String documentRef) {}

    public record AttestationVerifiee(UUID liaisonId, String type, boolean conforme, String motif) {}

    public record AttestationArchivee(String type, String documentRef) {}

    public record AttestationsArchivees(UUID liaisonId, List<AttestationArchivee> attestations) {}

    public record VisaSigne(UUID liaisonId, String nature, boolean favorable) {}

    public record VisasRequis(UUID liaisonId, java.util.List<String> natures) {}

    public record SecuriteApprouvee(UUID liaisonId, Instant approuveeLe) {}
}
