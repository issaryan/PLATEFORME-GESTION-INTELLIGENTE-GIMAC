package cm.gimac.des.certification.domain.service;

import cm.gimac.des.certification.domain.CertificationException;
import cm.gimac.des.certification.domain.decision.ApprobationSecurite;
import cm.gimac.des.certification.domain.decision.AttestationSecurite;
import cm.gimac.des.certification.domain.decision.VisaSecurite;
import cm.gimac.des.certification.domain.enumeration.EtatApprobation;
import cm.gimac.des.certification.domain.enumeration.EtatFicheChangement;
import cm.gimac.des.certification.domain.enumeration.EtatVisa;
import cm.gimac.des.certification.domain.enumeration.NatureVisa;
import cm.gimac.des.certification.domain.enumeration.TypeAttestation;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Règles du verrou 2 (D-56, rangs 20 à 26) : attestations exigées selon l'activité, état déduit des faits,
 * actions permises, conditions de l'approbation (dossier, attestations vérifiées et archivées, fiche lue
 * approuvée, visas actifs favorables). Reprises du serveur de substitution de l'interface.
 */
public final class ApprobationPolicy {

    /** Référence de fiche du dispositif de gestion des changements, par exemple CHG-2026-0412. */
    private static final Pattern REFERENCE_FICHE = Pattern.compile("^[A-Z]{2,5}-\\d{4}-\\d{1,6}$");

    private ApprobationPolicy() {}

    /** Modèles des types de projet (D-91) ; un type créé hérite de l'un d'eux. */
    public static final List<String> MODELES = List.of("CARTE_INTERBANCAIRE", "PAIEMENT_MOBILE", "DELEGATAIRE", "GENERIQUE");

    /**
     * Modèle du type de projet (D-91) : celui transmis par MS-HOMOLOGATION, sinon celui du type repris de même code
     * (carte interbancaire, paiement mobile, délégataire), sinon générique. Sans type connu, la règle historique
     * (celle de la carte interbancaire) s'applique.
     */
    public static String modele(String modeleTypeProjet, String typeProjet) {
        if (modeleTypeProjet != null && MODELES.contains(modeleTypeProjet)) {
            return modeleTypeProjet;
        }
        if (typeProjet == null) {
            return "CARTE_INTERBANCAIRE";
        }
        return MODELES.contains(typeProjet) ? typeProjet : "GENERIQUE";
    }

    /**
     * Attestations exigées (rangs 21 et 22) selon le modèle du type de projet (D-91) : PCI DSS pour la carte
     * interbancaire et le délégataire, ISO 27001 et GSMA pour le paiement mobile, aucune pour le modèle générique ;
     * conformité COBAC en plus pour une fintech. Le code d'un type repris vaut son modèle.
     */
    public static List<TypeAttestation> attestationsExigees(String modele, boolean fintech) {
        List<TypeAttestation> l = new ArrayList<>(
            switch (modele(modele, modele)) {
                case "PAIEMENT_MOBILE" -> List.of(TypeAttestation.ISO_27001, TypeAttestation.GSMA);
                case "GENERIQUE" -> List.<TypeAttestation>of();
                default -> List.of(TypeAttestation.PCI_DSS);
            }
        );
        if (fintech) {
            l.add(TypeAttestation.COBAC);
        }
        return l;
    }

    /** Une signature ou un dépôt n'est plus possible une fois la sécurité approuvée ou le changement refusé. */
    public static void verifierOuverte(ApprobationSecurite a) {
        EtatApprobation e = etat(a);
        if (e == EtatApprobation.APPROUVEE || e == EtatApprobation.REFUSEE) {
            throw CertificationException.conflit("CER_SECURITY_APPROVAL_CLOSED", "L'approbation de la sécurité est close.");
        }
    }

    /** Attestations figées par l'archivage (rang 23). */
    public static void verifierNonArchivee(ApprobationSecurite a) {
        if (a.estArchivee()) {
            throw CertificationException.conflit("CER_ATTESTATIONS_ARCHIVED", "Les attestations sont vérifiées et archivées.");
        }
    }

    public static void verifierReferenceFiche(String reference) {
        if (reference == null || !REFERENCE_FICHE.matcher(reference).matches()) {
            throw CertificationException.irrecevable(
                "CER_CHANGE_RECORD_INVALID",
                "Référence de fiche de changement invalide (exemple : CHG-2026-0412)."
            );
        }
    }

    /** Rejet ou avis défavorable : motif obligatoire. */
    public static void verifierMotif(boolean favorable, String motif, String message) {
        if (!favorable && (motif == null || motif.isBlank())) {
            throw CertificationException.irrecevable("CER_REJECTION_REASON_REQUIRED", message);
        }
    }

    public static boolean visasFavorables(ApprobationSecurite a) {
        return a.getVisas().stream().allMatch(v -> !v.isActif() || v.getEtat() == EtatVisa.SIGNE_FAVORABLE);
    }

    public static boolean visaDefavorable(ApprobationSecurite a) {
        return a.getVisas().stream().anyMatch(v -> v.isActif() && v.getEtat() == EtatVisa.SIGNE_DEFAVORABLE);
    }

    public static boolean toutesVerifiees(ApprobationSecurite a) {
        return a.getAttestations().stream().allMatch(AttestationSecurite::estVerifiee);
    }

    /** Conditions de l'approbation, lue sur la fiche : dossier, archivage, fiche approuvée, visas actifs favorables. */
    public static boolean approuvable(ApprobationSecurite a) {
        return (
            a.aDossier() && a.estArchivee() && a.getFicheEtat() == EtatFicheChangement.APPROUVEE && visasFavorables(a) && !a.estApprouvee()
        );
    }

    /** État déduit des faits, dans l'ordre de l'avancement. */
    public static EtatApprobation etat(ApprobationSecurite a) {
        if (a.estApprouvee()) {
            return EtatApprobation.APPROUVEE;
        }
        if (a.getFicheEtat() == EtatFicheChangement.REFUSEE || visaDefavorable(a)) {
            return EtatApprobation.REFUSEE;
        }
        if (a.estArchivee()) {
            return EtatApprobation.VERIFIE_ARCHIVE;
        }
        boolean verificationEntamee = a.getAttestations().stream().anyMatch(x -> x.estVerifiee() || x.getMotifRejet() != null);
        if (a.aDossier() && verificationEntamee) {
            return EtatApprobation.EN_VERIFICATION;
        }
        return a.aDossier() ? EtatApprobation.DOSSIER_CONSTITUE : EtatApprobation.EN_ATTENTE_DOSSIER;
    }

    /** La fiche est à relire si sa lecture est ancienne ou antérieure à l'archivage, tant que l'approbation n'est pas acquise. */
    public static boolean ficheARelire(ApprobationSecurite a, Instant maintenant, Duration fraicheur) {
        if (a.getFicheReference() == null || a.estApprouvee() || a.getFicheLueLe() == null) {
            return false;
        }
        boolean perimee = a.getFicheLueLe().plus(fraicheur).isBefore(maintenant);
        boolean anterieure = a.getArchiveeLe() != null && a.getFicheLueLe().isBefore(a.getArchiveeLe());
        return perimee || anterieure;
    }

    /**
     * Actions du DES, filtrées par les permissions du jeton : CONSTITUER_DOSSIER, VERIFIER_ATTESTATION, ARCHIVER,
     * REFERENCER_FICHE, RELIRE_FICHE, SIGNER_VISA_CONFORMITE, SIGNER_VISA_DG.
     */
    public static List<String> actionsDes(ApprobationSecurite a, boolean pvSigne, Set<String> permissions) {
        List<String> actions = new ArrayList<>();
        EtatApprobation e = etat(a);
        boolean ouverte = pvSigne && e != EtatApprobation.APPROUVEE && e != EtatApprobation.REFUSEE;
        boolean lier = permissions.contains(PermissionsDecision.LIER_CHANGEMENT);
        boolean securite = permissions.contains(PermissionsDecision.VERIFIER_SECURITE);
        if (ouverte && !a.aDossier() && lier) {
            actions.add("CONSTITUER_DOSSIER");
        }
        if (ouverte && !a.estArchivee() && securite && a.getAttestations().stream().anyMatch(x -> x.estFournie() && !x.estVerifiee())) {
            actions.add("VERIFIER_ATTESTATION");
        }
        if (ouverte && !a.estArchivee() && securite && toutesVerifiees(a)) {
            actions.add("ARCHIVER");
        }
        if (ouverte && a.aDossier() && a.getFicheReference() == null && lier) {
            actions.add("REFERENCER_FICHE");
        }
        if (ouverte && a.getFicheReference() != null && (lier || securite)) {
            actions.add("RELIRE_FICHE");
        }
        if (ouverte) {
            for (VisaSecurite v : a.getVisas()) {
                if (v.isActif() && v.getEtat() == EtatVisa.ATTENDU) {
                    if (v.getNature() == NatureVisa.CONFORMITE && permissions.contains(PermissionsDecision.VISA_CONFORMITE)) {
                        actions.add("SIGNER_VISA_CONFORMITE");
                    } else if (v.getNature() == NatureVisa.DIRECTION_GENERALE && permissions.contains(PermissionsDecision.VISA_DG)) {
                        actions.add("SIGNER_VISA_DG");
                    }
                }
            }
        }
        return actions;
    }

    /** Action du portail : DEPOSER_ATTESTATION tant qu'une attestation exigée manque et que rien n'est archivé. */
    public static List<String> actionsPortail(ApprobationSecurite a, boolean pvSigne, Set<String> permissions) {
        EtatApprobation e = etat(a);
        boolean manque = a.getAttestations().stream().anyMatch(x -> !x.estFournie());
        return pvSigne &&
            manque &&
            !a.estArchivee() &&
            e != EtatApprobation.APPROUVEE &&
            e != EtatApprobation.REFUSEE &&
            permissions.contains(PermissionsDecision.PORTAIL_DEPOSER)
            ? List.of("DEPOSER_ATTESTATION")
            : List.of();
    }
}
