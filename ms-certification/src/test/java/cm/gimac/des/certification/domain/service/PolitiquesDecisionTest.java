package cm.gimac.des.certification.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.certification.domain.CertificationException;
import cm.gimac.des.certification.domain.decision.ApprobationSecurite;
import cm.gimac.des.certification.domain.decision.ListeCodesConverter;
import cm.gimac.des.certification.domain.enumeration.EtatApprobation;
import cm.gimac.des.certification.domain.enumeration.EtatFicheChangement;
import cm.gimac.des.certification.domain.enumeration.EtatProcesVerbal;
import cm.gimac.des.certification.domain.enumeration.NatureVisa;
import cm.gimac.des.certification.domain.enumeration.RoleSignature;
import cm.gimac.des.certification.domain.enumeration.TypeAttestation;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Règles pures des verrous (D-56) : établissement, signatures, attestations exigées, état et actions. */
class PolitiquesDecisionTest {

    static final Set<String> TOUTES = Set.of(
        PermissionsDecision.ETABLIR_PV,
        PermissionsDecision.SIGNER_PV_GIMAC,
        PermissionsDecision.LIER_CHANGEMENT,
        PermissionsDecision.VERIFIER_SECURITE,
        PermissionsDecision.VISA_CONFORMITE,
        PermissionsDecision.VISA_DG,
        PermissionsDecision.PORTAIL_SIGNER,
        PermissionsDecision.PORTAIL_DEPOSER
    );

    @ParameterizedTest(name = "{0} {1} : {2}")
    @CsvSource(
        {
            "ETABLI, GIMAC, SIGNE_GIMAC",
            "SIGNE_PARTICIPANT, GIMAC, SIGNE",
            "ETABLI, PARTICIPANT, SIGNE_PARTICIPANT",
            "SIGNE_GIMAC, PARTICIPANT, SIGNE",
        }
    )
    @DisplayName("Signatures du procès-verbal dans un ordre quelconque")
    void signatures(EtatProcesVerbal avant, RoleSignature role, EtatProcesVerbal apres) {
        assertThat(ProcesVerbalPolicy.apresSignature(avant, role)).isEqualTo(apres);
        assertThat(ProcesVerbalPolicy.attendSignature(avant, role)).isTrue();
    }

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource(
        {
            "NON_ETABLI, GIMAC",
            "SIGNE_GIMAC, GIMAC",
            "SIGNE, GIMAC",
            "NON_ETABLI, PARTICIPANT",
            "SIGNE_PARTICIPANT, PARTICIPANT",
            "SIGNE, PARTICIPANT",
            "ETABLI, CONFORMITE",
        }
    )
    @DisplayName("Signature hors de propos : CER_PV_NOT_SIGNABLE ; une signature ne se refait pas")
    void nonSignable(EtatProcesVerbal etat, RoleSignature role) {
        assertThat(ProcesVerbalPolicy.attendSignature(etat, role)).isFalse();
        assertThatThrownBy(() -> ProcesVerbalPolicy.apresSignature(etat, role))
            .isInstanceOfSatisfying(CertificationException.class, e -> assertThat(e.code()).isEqualTo("CER_PV_NOT_SIGNABLE"));
    }

    @Test
    @DisplayName("Établissement : 100 % exigés avant le constat ; taux absent compté 0")
    void etablissement() {
        assertThatThrownBy(() -> ProcesVerbalPolicy.verifierEtablissement(null, true, EtatProcesVerbal.NON_ETABLI))
            .hasMessageContaining("0 %")
            .isInstanceOfSatisfying(CertificationException.class, e -> assertThat(e.status()).isEqualTo(409));
        assertThatThrownBy(() -> ProcesVerbalPolicy.verifierEtablissement(100, true, EtatProcesVerbal.ETABLI))
            .isInstanceOfSatisfying(CertificationException.class, e -> assertThat(e.code()).isEqualTo("CER_PV_NOT_READY"));
        ProcesVerbalPolicy.verifierEtablissement(100, true, EtatProcesVerbal.NON_ETABLI);
        assertThat(ProcesVerbalPolicy.actionsDes(EtatProcesVerbal.NON_ETABLI, true, null, TOUTES)).containsExactly("ETABLIR_BLOQUE");
        assertThat(ProcesVerbalPolicy.actionsDes(EtatProcesVerbal.SIGNE, true, 100, TOUTES)).isEmpty();
        assertThat(ProcesVerbalPolicy.actionsPortail(EtatProcesVerbal.ETABLI, TOUTES)).containsExactly("SIGNER_PARTICIPANT");
        assertThat(ProcesVerbalPolicy.actionsPortail(EtatProcesVerbal.SIGNE_PARTICIPANT, TOUTES)).isEmpty();
    }

    @Test
    @DisplayName("Attestations exigées selon l'activité : carte et délégataire PCI DSS, mobile ISO 27001 et GSMA, fintech COBAC")
    void attestationsExigees() {
        assertThat(ApprobationPolicy.attestationsExigees("CARTE_INTERBANCAIRE", false)).containsExactly(TypeAttestation.PCI_DSS);
        assertThat(ApprobationPolicy.attestationsExigees("DELEGATAIRE", false)).containsExactly(TypeAttestation.PCI_DSS);
        assertThat(ApprobationPolicy.attestationsExigees("PAIEMENT_MOBILE", false)).containsExactly(TypeAttestation.ISO_27001, TypeAttestation.GSMA);
        assertThat(ApprobationPolicy.attestationsExigees("PAIEMENT_MOBILE", true)).containsExactly(
            TypeAttestation.ISO_27001,
            TypeAttestation.GSMA,
            TypeAttestation.COBAC
        );
    }

    @Test
    @DisplayName("État déduit des faits et actions : chaque acteur ne reçoit que ses gestes")
    void etatEtActions() {
        ApprobationSecurite a = new ApprobationSecurite(UUID.randomUUID(), "I", "CARTE_INTERBANCAIRE", false, List.of(TypeAttestation.PCI_DSS), Set.of(NatureVisa.CONFORMITE));
        Instant t = Instant.parse("2026-09-30T08:00:00Z");
        assertThat(ApprobationPolicy.etat(a)).isEqualTo(EtatApprobation.EN_ATTENTE_DOSSIER);
        assertThat(ApprobationPolicy.actionsDes(a, false, TOUTES)).isEmpty();
        assertThat(ApprobationPolicy.actionsDes(a, true, TOUTES)).containsExactly("CONSTITUER_DOSSIER", "SIGNER_VISA_CONFORMITE");
        assertThat(ApprobationPolicy.actionsDes(a, true, Set.of())).isEmpty();
        assertThat(ApprobationPolicy.actionsPortail(a, true, TOUTES)).containsExactly("DEPOSER_ATTESTATION");
        assertThat(ApprobationPolicy.actionsPortail(a, false, TOUTES)).isEmpty();
        assertThat(ApprobationPolicy.actionsPortail(a, true, Set.of())).isEmpty();
        a.attestation(TypeAttestation.PCI_DSS).orElseThrow().deposer("doc", "pci.pdf", "e", t, "Corr");
        assertThat(ApprobationPolicy.etat(a)).isEqualTo(EtatApprobation.EN_ATTENTE_DOSSIER);
        assertThat(ApprobationPolicy.actionsDes(a, true, TOUTES)).contains("VERIFIER_ATTESTATION");
        a.attestation(TypeAttestation.PCI_DSS).orElseThrow().verifierConforme(t, "r", "RSSI");
        assertThat(ApprobationPolicy.etat(a)).isEqualTo(EtatApprobation.EN_ATTENTE_DOSSIER);
        a.constituerDossier("d", "d.pdf", "e", t, "p", "Prod");
        assertThat(ApprobationPolicy.etat(a)).isEqualTo(EtatApprobation.EN_VERIFICATION);
        assertThat(ApprobationPolicy.approuvable(a)).isFalse();
        a.archiver(t);
        a.referencerFiche("CHG-2026-0001", EtatFicheChangement.APPROUVEE, t);
        assertThat(ApprobationPolicy.visasFavorables(a)).isFalse();
        assertThat(ApprobationPolicy.approuvable(a)).isFalse();
        a.visa(NatureVisa.CONFORMITE).orElseThrow().signer(true, null, UUID.randomUUID());
        assertThat(ApprobationPolicy.approuvable(a)).isTrue();
        assertThat(ApprobationPolicy.ficheARelire(a, t.plus(Duration.ofHours(1)), Duration.ofMinutes(15))).isTrue();
        a.approuver(t);
        assertThat(ApprobationPolicy.approuvable(a)).isFalse();
        assertThat(ApprobationPolicy.etat(a)).isEqualTo(EtatApprobation.APPROUVEE);
        assertThat(ApprobationPolicy.ficheARelire(a, t.plus(Duration.ofHours(1)), Duration.ofMinutes(15))).isFalse();
        assertThat(ApprobationPolicy.actionsDes(a, true, TOUTES)).isEmpty();
        assertThatThrownBy(() -> ApprobationPolicy.verifierOuverte(a))
            .isInstanceOfSatisfying(CertificationException.class, e -> assertThat(e.code()).isEqualTo("CER_SECURITY_APPROVAL_CLOSED"));
    }

    @Test
    @DisplayName("Références de fiche et motifs")
    void referencesEtMotifs() {
        ApprobationPolicy.verifierReferenceFiche("CHG-2026-0412");
        ApprobationPolicy.verifierReferenceFiche("RFC-2026-1");
        for (String r : new String[] { "chg-2026-1", "CHG-26-1", "CHG-2026-1234567", "C-2026-1" }) {
            assertThatThrownBy(() -> ApprobationPolicy.verifierReferenceFiche(r)).isInstanceOf(CertificationException.class);
        }
        ApprobationPolicy.verifierMotif(true, null, "m");
        ApprobationPolicy.verifierMotif(false, "motif", "m");
        assertThatThrownBy(() -> ApprobationPolicy.verifierMotif(false, null, "Un rejet doit être motivé.")).hasMessage("Un rejet doit être motivé.");
    }

    @Test
    @DisplayName("Décision canonique : ordre fixe, valeurs absentes vides, empreinte SHA-256")
    void canonique() {
        DecisionCanonique d = DecisionCanonique.de("X").avec("a", 1).avec("b", null).avec("c", "l1\nl2");
        assertThat(d.texte()).isEqualTo("nature=X\na=1\nb=\nc=l1 l2\n");
        assertThat(d.empreinte()).isEqualTo(DecisionCanonique.sha256(d.texte())).matches("[0-9a-f]{64}");
        assertThat(DecisionCanonique.sha256("")).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    @DisplayName("Exceptions métier : statuts et codes ; liste de codes en une colonne")
    void exceptionsEtConvertisseur() {
        assertThat(CertificationException.conflit("C", "m", Map.of("k", "v")).details()).containsEntry("k", "v");
        assertThat(CertificationException.indisponible("U", "m").status()).isEqualTo(503);
        assertThat(CertificationException.reauthentificationExigee().status()).isEqualTo(401);
        ListeCodesConverter conv = new ListeCodesConverter();
        assertThat(conv.convertToDatabaseColumn(List.of("A", "B"))).isEqualTo("A,B");
        assertThat(conv.convertToDatabaseColumn(List.of())).isNull();
        assertThat(conv.convertToDatabaseColumn(null)).isNull();
        assertThat(conv.convertToEntityAttribute("A, B,")).containsExactly("A", "B");
        assertThat(conv.convertToEntityAttribute(" ")).isEmpty();
        assertThat(conv.convertToEntityAttribute(null)).isEmpty();
    }
}
