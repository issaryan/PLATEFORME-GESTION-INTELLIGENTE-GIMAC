package cm.gimac.des.certification.application.usecase;

import static cm.gimac.des.certification.application.usecase.BancDecision.AUTRE;
import static cm.gimac.des.certification.application.usecase.BancDecision.DOC_SHA;
import static cm.gimac.des.certification.application.usecase.BancDecision.INSTITUTION;
import static cm.gimac.des.certification.application.usecase.BancDecision.conducteur;
import static cm.gimac.des.certification.application.usecase.BancDecision.conformite;
import static cm.gimac.des.certification.application.usecase.BancDecision.correspondant;
import static cm.gimac.des.certification.application.usecase.BancDecision.dg;
import static cm.gimac.des.certification.application.usecase.BancDecision.pdf;
import static cm.gimac.des.certification.application.usecase.BancDecision.production;
import static cm.gimac.des.certification.application.usecase.BancDecision.rssi;
import static cm.gimac.des.certification.application.usecase.ProcesVerbalServiceTest.refus;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.application.port.in.Acteur;
import cm.gimac.des.certification.application.port.out.DocumentPort.Fichier;
import cm.gimac.des.certification.application.usecase.DecisionVues.ApprobationVue;
import cm.gimac.des.certification.domain.enumeration.EtatFicheChangement;
import cm.gimac.des.certification.domain.enumeration.NatureVisa;
import cm.gimac.des.certification.domain.event.DecisionEvenements;
import cm.gimac.des.certification.infrastructure.client.ReauthentificationStubAdapter;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Verrou 2 (D-56) : dossier et fiche de changement, attestations, visas, approbation ; portail. */
class ApprobationSecuriteServiceTest {

    final BancDecision banc = new BancDecision(Set.of("DELEGATAIRE"), Set.of("DELEGATAIRE"));

    static Fichier fichier(String nom) {
        return new Fichier(nom, "application/pdf", pdf());
    }

    ApprobationVue dossier(UUID id) {
        UUID doc = UUID.randomUUID();
        banc.documents.connaitre(doc, DOC_SHA);
        return banc.approbations.constituerDossier(id, doc, DOC_SHA, "dossier-changement.pdf", production());
    }

    @Test
    @DisplayName("Avant la signature du PV : approbation en attente, attestations exigées, aucune action ; actes refusés")
    void avantSignature() {
        UUID id = banc.liaison("CARTE_INTERBANCAIRE", true, 100, true).liaisonId();
        ApprobationVue v = banc.approbations.consulter(id, production());
        assertThat(v.etat()).isEqualTo("EN_ATTENTE_DOSSIER");
        assertThat(v.attestations()).extracting(DecisionVues.AttestationVue::type).containsExactly("PCI_DSS", "COBAC");
        assertThat(v.fintech()).isTrue();
        assertThat(v.visas()).extracting(DecisionVues.VisaVue::actif).containsExactly(false, false);
        assertThat(v.actionsPermises()).isEmpty();
        refus(() -> dossier(id), 409, "CER_PV_NOT_SIGNED");
        refus(() -> banc.approbations.deposerAttestation(id, "PCI_DSS", fichier("pci.pdf"), correspondant(INSTITUTION)), 409, "CER_PV_NOT_SIGNED");
        refus(() -> banc.approbations.deposerAttestation(id, "PCI_DSS", fichier("pci.pdf"), correspondant(AUTRE)), 404, "HOM_LIAISON_NOT_FOUND");
        refus(() -> banc.approbations.consulterPortail(id, correspondant(AUTRE)), 404, "HOM_LIAISON_NOT_FOUND");
        assertThat(banc.approbations.consulterPortail(id, correspondant(INSTITUTION)).actionsPermises()).isEmpty();
        refus(() -> banc.approbations.consulter(UUID.randomUUID(), rssi()), 404, "HOM_LIAISON_NOT_FOUND");
        refus(() -> banc.approbations.archiver(UUID.randomUUID(), rssi()), 404, "HOM_LIAISON_NOT_FOUND");
        // PV établi mais pas signé : les attestations viennent du PV, sans nouvel appel à MS-HOMOLOGATION.
        banc.procesVerbaux.etablir(id, conducteur());
        banc.contextes.oublier(id);
        assertThat(banc.approbations.consulter(id, rssi()).attestations()).hasSize(2);
        refus(() -> banc.approbations.archiver(id, rssi()), 409, "CER_PV_NOT_SIGNED");
    }

    @Test
    @DisplayName("Parcours complet d'une carte : dossier, attestation PCI DSS, vérification, archivage, fiche lue : APPROUVEE")
    void parcoursComplet() {
        UUID id = banc.pvSigne("CARTE_INTERBANCAIRE", false);
        // Aucun visa activé pour une carte : aucun événement de visas attendus.
        assertThat(banc.evenements).noneMatch(e -> e.type().equals(DecisionEvenements.VISAS_REQUIS));
        ApprobationVue v = banc.approbations.consulter(id, production());
        assertThat(v.actionsPermises()).containsExactly("CONSTITUER_DOSSIER");
        v = dossier(id);
        assertThat(v.etat()).isEqualTo("DOSSIER_CONSTITUE");
        assertThat(v.dossierChangement().deposePar()).isEqualTo("Production monétique");
        assertThat(v.actionsPermises()).containsExactly("REFERENCER_FICHE");
        refus(() -> dossier(id), 409, "CER_CHANGE_DOSSIER_ALREADY_CONSTITUTED");
        Acteur corr = correspondant(INSTITUTION);
        assertThat(banc.approbations.consulterPortail(id, corr).actionsPermises()).containsExactly("DEPOSER_ATTESTATION");
        v = banc.approbations.deposerAttestation(id, "PCI_DSS", fichier("pci.pdf"), corr);
        assertThat(v.attestations().get(0).preuve().empreinte()).matches("[0-9a-f]{64}");
        assertThat(v.actionsPermises()).isEmpty();
        assertThat(v.visas()).isEmpty();
        assertThat(v.dossierChangement().deposePar()).isEqualTo("PRODUCTION_MONETIQUE");
        assertThat(banc.approbations.consulter(id, rssi()).actionsPermises()).containsExactly("VERIFIER_ATTESTATION");
        v = banc.approbations.verifierAttestation(id, "PCI_DSS", true, "", rssi());
        assertThat(v.etat()).isEqualTo("EN_VERIFICATION");
        assertThat(v.attestations().get(0).verifieePar()).isEqualTo("RSSI");
        assertThat(v.actionsPermises()).containsExactly("ARCHIVER");
        assertThat(banc.approbations.consulterPortail(id, corr).attestations().get(0).verifieePar()).isEqualTo("RSSI");
        refus(() -> banc.approbations.deposerAttestation(id, "PCI_DSS", fichier("pci.pdf"), corr), 409, "CER_ATTESTATION_ALREADY_VERIFIED");
        v = banc.approbations.archiver(id, rssi());
        assertThat(v.etat()).isEqualTo("VERIFIE_ARCHIVE");
        assertThat(v.attestations().get(0).archiveeLe()).isNotNull();
        refus(() -> banc.approbations.archiver(id, rssi()), 409, "CER_ATTESTATIONS_ARCHIVED");
        refus(() -> banc.approbations.verifierAttestation(id, "PCI_DSS", true, "", rssi()), 409, "CER_ATTESTATIONS_ARCHIVED");
        refus(() -> banc.approbations.relireFiche(id, production()), 409, "CER_CHANGE_RECORD_MISSING");
        v = banc.approbations.referencerFiche(id, "CHG-2026-0413", production());
        assertThat(v.etat()).isEqualTo("APPROUVEE");
        assertThat(v.approuveeLe()).hasToString("2026-09-30");
        assertThat(v.ficheChangement().etatApprobation()).isEqualTo("APPROUVEE");
        assertThat(v.ficheChangement().aRelire()).isFalse();
        assertThat(v.actionsPermises()).isEmpty();
        refus(() -> banc.approbations.referencerFiche(id, "CHG-2026-0414", production()), 409, "CER_SECURITY_APPROVAL_CLOSED");
        assertThat(banc.approbations.relireFiche(id, production()).etat()).isEqualTo("APPROUVEE");
        assertThat(banc.types()).containsSubsequence(
            DecisionEvenements.DOSSIER_CHANGEMENT_CONSTITUE,
            DecisionEvenements.ATTESTATION_DEPOSEE,
            DecisionEvenements.ATTESTATION_VERIFIEE,
            DecisionEvenements.ATTESTATIONS_ARCHIVEES,
            DecisionEvenements.FICHE_CHANGEMENT_REFERENCEE,
            DecisionEvenements.SECURITE_APPROUVEE
        );
        var archivees = banc.evenements.stream().filter(e -> e.type().equals(DecisionEvenements.ATTESTATIONS_ARCHIVEES)).findFirst().orElseThrow();
        assertThat(((DecisionEvenements.AttestationsArchivees) archivees.charge()).attestations()).extracting(DecisionEvenements.AttestationArchivee::type).containsExactly("PCI_DSS");
        assertThat(archivees.institution()).isEqualTo(INSTITUTION);
    }

    @Test
    @DisplayName("Rejet motivé : motif exigé, pièce écartée, nouveau dépôt attendu")
    void rejetMotive() {
        UUID id = banc.pvSigne("PAIEMENT_MOBILE", true);
        dossier(id);
        Acteur corr = correspondant(INSTITUTION);
        refus(() -> banc.approbations.verifierAttestation(id, "GSMA", true, "", rssi()), 409, "CER_ATTESTATION_MISSING");
        refus(() -> banc.approbations.verifierAttestation(id, "PCI_DSS", true, "", rssi()), 422, "CER_ATTESTATION_NOT_REQUIRED");
        refus(() -> banc.approbations.deposerAttestation(id, "PCI_DSS", fichier("pci.pdf"), corr), 422, "CER_ATTESTATION_NOT_REQUIRED");
        refus(() -> banc.approbations.deposerAttestation(id, "INCONNU", fichier("x.pdf"), corr), 422, "CER_ATTESTATION_NOT_REQUIRED");
        refus(() -> banc.approbations.deposerAttestation(id, "GSMA", new Fichier("g.png", "image/png", new byte[] { 1, 2 }), corr), 422, "CER_ATTESTATION_FORMAT");
        refus(() -> banc.approbations.deposerAttestation(id, "GSMA", new Fichier("g.pdf", "application/pdf", new byte[] { 1 }), corr), 422, "CER_ATTESTATION_FORMAT");
        refus(() -> banc.approbations.deposerAttestation(id, "GSMA", null, corr), 422, "CER_ATTESTATION_FORMAT");
        banc.approbations.deposerAttestation(id, "GSMA", fichier("gsma.pdf"), corr);
        refus(() -> banc.approbations.verifierAttestation(id, "GSMA", false, "  ", rssi()), 422, "CER_REJECTION_REASON_REQUIRED");
        ApprobationVue v = banc.approbations.verifierAttestation(id, "GSMA", false, "Attestation expirée", rssi());
        var gsma = v.attestations().stream().filter(a -> a.type().equals("GSMA")).findFirst().orElseThrow();
        assertThat(gsma.preuve()).isNull();
        assertThat(gsma.motifRejet()).isEqualTo("Attestation expirée");
        assertThat(v.etat()).isEqualTo("EN_VERIFICATION");
        refus(() -> banc.approbations.archiver(id, rssi()), 409, "CER_ATTESTATIONS_INCOMPLETE");
        assertThat(banc.approbations.consulterPortail(id, corr).actionsPermises()).containsExactly("DEPOSER_ATTESTATION");
        v = banc.approbations.deposerAttestation(id, "GSMA", fichier("gsma-2026.pdf"), corr);
        assertThat(v.attestations().stream().filter(a -> a.type().equals("GSMA")).findFirst().orElseThrow().motifRejet()).isNull();
        var verifiee = (DecisionEvenements.AttestationVerifiee) banc.evenements.stream().filter(e -> e.type().equals(DecisionEvenements.ATTESTATION_VERIFIEE)).findFirst().orElseThrow().charge();
        assertThat(verifiee.conforme()).isFalse();
        assertThat(verifiee.motif()).isEqualTo("Attestation expirée");
    }

    @Test
    @DisplayName("Fiche de changement : référence contrôlée, lue jamais refaite, à relire après l'archivage ; refus du dispositif : REFUSEE")
    void ficheChangement() {
        UUID id = banc.pvSigne("CARTE_INTERBANCAIRE", false);
        refus(() -> banc.approbations.referencerFiche(id, "mauvaise", production()), 422, "CER_CHANGE_RECORD_INVALID");
        refus(() -> banc.approbations.referencerFiche(id, null, production()), 422, "CER_CHANGE_RECORD_INVALID");
        dossier(id);
        ApprobationVue v = banc.approbations.referencerFiche(id, "CHG-2026-0412", production());
        assertThat(v.etat()).isEqualTo("DOSSIER_CONSTITUE");
        assertThat(v.ficheChangement().aRelire()).isFalse();
        assertThat(v.actionsPermises()).containsExactly("RELIRE_FICHE");
        assertThat(banc.approbations.consulter(id, rssi()).actionsPermises()).containsExactly("RELIRE_FICHE");
        banc.horloge.avancer(Duration.ofMinutes(20));
        assertThat(banc.approbations.consulter(id, production()).ficheChangement().aRelire()).isTrue();
        banc.approbations.deposerAttestation(id, "PCI_DSS", fichier("pci.pdf"), correspondant(INSTITUTION));
        banc.approbations.verifierAttestation(id, "PCI_DSS", true, null, rssi());
        banc.approbations.archiver(id, rssi());
        banc.horloge.avancer(Duration.ofMinutes(-19));
        assertThat(banc.approbations.consulter(id, production()).ficheChangement().aRelire()).isTrue();
        banc.fiches.enregistrer("CHG-2026-0412", EtatFicheChangement.REFUSEE);
        v = banc.approbations.relireFiche(id, production());
        assertThat(v.etat()).isEqualTo("REFUSEE");
        assertThat(v.actionsPermises()).isEmpty();
        refus(() -> banc.approbations.archiver(id, rssi()), 409, "CER_SECURITY_APPROVAL_CLOSED");
        banc.fiches.enregistrer("CHG-2026-0412", EtatFicheChangement.APPROUVEE);
        assertThat(banc.approbations.relireFiche(id, rssi()).etat()).isEqualTo("APPROUVEE");
    }

    @Test
    @DisplayName("Visas activés (délégataire) : insérés avant l'approbation, signés avec assertion, immuables")
    void visas() {
        UUID id = banc.pvSigne("DELEGATAIRE", false);
        // Ouverture du verrou 2 : visas attendus publiés pour les tableaux de bord de la conformité et de la DG.
        var requis = (DecisionEvenements.VisasRequis) banc.evenements.stream().filter(e -> e.type().equals(DecisionEvenements.VISAS_REQUIS)).findFirst().orElseThrow().charge();
        assertThat(requis.liaisonId()).isEqualTo(id);
        assertThat(requis.natures()).containsExactly("CONFORMITE", "DIRECTION_GENERALE");
        ApprobationVue v = banc.approbations.consulter(id, conformite());
        assertThat(v.visas()).extracting(DecisionVues.VisaVue::actif).containsExactly(true, true);
        assertThat(v.actionsPermises()).containsExactly("SIGNER_VISA_CONFORMITE");
        assertThat(banc.approbations.consulter(id, dg()).actionsPermises()).containsExactly("SIGNER_VISA_DG");
        refus(() -> banc.approbations.signerVisa(id, NatureVisa.CONFORMITE, true, "", conformite(), ""), 401, "IAM_REAUTH_REQUIRED");
        refus(() -> banc.approbations.signerVisa(id, NatureVisa.CONFORMITE, false, " ", conformite(), "a"), 422, "CER_REJECTION_REASON_REQUIRED");
        refus(
            () -> banc.approbations.signerVisa(id, NatureVisa.CONFORMITE, true, "", conformite(), ReauthentificationStubAdapter.ASSERTION_REFUSEE),
            403,
            "CER_REAUTH_INVALID"
        );
        v = banc.approbations.signerVisa(id, NatureVisa.CONFORMITE, true, "Conforme", conformite(), "a");
        var visa = v.visas().stream().filter(x -> x.nature().equals("CONFORMITE")).findFirst().orElseThrow();
        assertThat(visa.etat()).isEqualTo("SIGNE_FAVORABLE");
        assertThat(visa.signature().signataire()).isEqualTo("Conformité");
        assertThat(visa.signature().fonction()).isEqualTo("CONFORMITE");
        assertThat(visa.motif()).isEqualTo("Conforme");
        refus(() -> banc.approbations.signerVisa(id, NatureVisa.CONFORMITE, true, "", conformite(), "a"), 409, "CER_VISA_ALREADY_SIGNED");
        // Tout est réuni sauf le visa DG : pas d'approbation.
        dossier(id);
        banc.approbations.deposerAttestation(id, "PCI_DSS", fichier("pci.pdf"), correspondant(INSTITUTION));
        banc.approbations.verifierAttestation(id, "PCI_DSS", true, "", rssi());
        banc.approbations.archiver(id, rssi());
        assertThat(banc.approbations.referencerFiche(id, "CHG-2026-0415", production()).etat()).isEqualTo("VERIFIE_ARCHIVE");
        v = banc.approbations.signerVisa(id, NatureVisa.DIRECTION_GENERALE, true, null, dg(), "a");
        assertThat(v.etat()).isEqualTo("VERIFIE_ARCHIVE");
        assertThat(banc.approbations.relireFiche(id, production()).etat()).isEqualTo("APPROUVEE");
        assertThat(banc.signaturesEnregistrees).hasSize(4);
        assertThat(banc.types()).contains(DecisionEvenements.VISA_SIGNE, DecisionEvenements.SECURITE_APPROUVEE);
    }

    @Test
    @DisplayName("Visa non requis (désactivé par défaut) ; avis défavorable motivé : approbation REFUSEE")
    void visaNonRequisEtDefavorable() {
        UUID carte = banc.pvSigne("CARTE_INTERBANCAIRE", false);
        refus(() -> banc.approbations.signerVisa(carte, NatureVisa.DIRECTION_GENERALE, true, "", dg(), "a"), 409, "CER_VISA_NOT_REQUIRED");
        refus(() -> banc.approbations.signerVisa(UUID.randomUUID(), NatureVisa.DIRECTION_GENERALE, true, "", dg(), "a"), 404, "HOM_LIAISON_NOT_FOUND");
        UUID id = banc.pvSigne("DELEGATAIRE", false);
        ApprobationVue v = banc.approbations.signerVisa(id, NatureVisa.DIRECTION_GENERALE, false, "Risque de place", dg(), "a");
        assertThat(v.etat()).isEqualTo("REFUSEE");
        refus(() -> banc.approbations.signerVisa(id, NatureVisa.CONFORMITE, true, "", conformite(), "a"), 409, "CER_SECURITY_APPROVAL_CLOSED");
    }

    @Test
    @DisplayName("Dossier : pièce inconnue ou empreinte divergente refusée (422 CER_DOCUMENT_INVALID)")
    void dossierInvalide() {
        UUID id = banc.pvSigne("CARTE_INTERBANCAIRE", false);
        UUID doc = UUID.randomUUID();
        banc.documents.connaitre(doc, "b".repeat(64));
        refus(() -> banc.approbations.constituerDossier(id, doc, DOC_SHA, "d.pdf", production()), 422, "CER_DOCUMENT_INVALID");
        refus(() -> banc.approbations.constituerDossier(id, null, DOC_SHA, "d.pdf", production()), 422, "CER_DOCUMENT_INVALID");
        refus(() -> banc.approbations.constituerDossier(id, doc, null, "d.pdf", production()), 422, "CER_DOCUMENT_INVALID");
        assertThat(banc.approbations.visasActifs(null)).isEmpty();
    }
}
