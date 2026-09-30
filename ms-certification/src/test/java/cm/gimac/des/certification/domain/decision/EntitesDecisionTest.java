package cm.gimac.des.certification.domain.decision;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.domain.enumeration.EtatApprobation;
import cm.gimac.des.certification.domain.enumeration.EtatFicheChangement;
import cm.gimac.des.certification.domain.enumeration.EtatVisa;
import cm.gimac.des.certification.domain.enumeration.NatureVisa;
import cm.gimac.des.certification.domain.enumeration.ObjetSignature;
import cm.gimac.des.certification.domain.enumeration.RoleSignature;
import cm.gimac.des.certification.domain.enumeration.TypeAttestation;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Entités des verrous : état initial, faits enregistrés et restitution fidèle. */
class EntitesDecisionTest {

    static final Instant T = Instant.parse("2026-09-30T08:00:00Z");

    @Test
    @DisplayName("Procès-verbal : non établi à la création, constat puis établissement figeant l'identité et la couverture")
    void procesVerbal() {
        UUID liaison = UUID.randomUUID();
        ProcesVerbalCertification pv = new ProcesVerbalCertification(liaison);
        assertThat(pv.getId()).isNotNull();
        assertThat(pv.estEtabli()).isFalse();
        assertThat(pv.isCouvertureConstatee()).isFalse();
        pv.constaterCouverture(100, List.of("RETRAIT"));
        assertThat(pv.isCouvertureConstatee()).isTrue();
        pv.etablir(
            new ProcesVerbalCertification.Identite("LIA-1", "I", "Banque", "CARTE_INTERBANCAIRE", true),
            new ProcesVerbalCertification.Couverture(100, 2, 2),
            List.of("RETRAIT", "PAIEMENT"),
            "u1",
            "Conducteur",
            T,
            "e"
        );
        assertThat(pv.estEtabli()).isTrue();
        assertThat(pv.getLiaisonId()).isEqualTo(liaison);
        assertThat(pv.getReference()).isEqualTo("LIA-1");
        assertThat(pv.getInstitutionId()).isEqualTo("I");
        assertThat(pv.getInstitutionNom()).isEqualTo("Banque");
        assertThat(pv.getTypeProjet()).isEqualTo("CARTE_INTERBANCAIRE");
        assertThat(pv.isFintech()).isTrue();
        assertThat(pv.getCouvertureNumerateur()).isEqualTo(2);
        assertThat(pv.getCouvertureDenominateur()).isEqualTo(2);
        assertThat(pv.getServicesCouverts()).containsExactly("RETRAIT", "PAIEMENT");
        assertThat(pv.getEtabliParId()).isEqualTo("u1");
        assertThat(pv.getEtabliParNom()).isEqualTo("Conducteur");
        assertThat(pv.getEtabliLe()).isEqualTo(T);
        assertThat(pv.getEmpreinte()).isEqualTo("e");
    }

    @Test
    @DisplayName("Signature : décision et scellement restitués ; fonction déduite de la qualité")
    void signature() {
        UUID id = UUID.randomUUID();
        UUID jeton = UUID.randomUUID();
        UUID liaison = UUID.randomUUID();
        SignatureVerrou s = new SignatureVerrou(
            id,
            new SignatureVerrou.Decision(liaison, ObjetSignature.VISA, RoleSignature.DIRECTION_GENERALE, "u", "DG", false, "Motif", T, "o"),
            new SignatureVerrou.Scellement("d", "a", "c", T, jeton)
        );
        assertThat(s.getId()).isEqualTo(id);
        assertThat(s.getLiaisonId()).isEqualTo(liaison);
        assertThat(s.getSignataireId()).isEqualTo("u");
        assertThat(s.getFonction()).isEqualTo("DG");
        assertThat(s.isFavorable()).isFalse();
        assertThat(s.getMotif()).isEqualTo("Motif");
        assertThat(s.getHorodatage()).isEqualTo(T);
        assertThat(s.getCachet()).isEqualTo("c");
        assertThat(s.getJetonHorodatageId()).isEqualTo(jeton);
    }

    @Test
    @DisplayName("Approbation : attestations exigées et visas créés à l'ouverture ; faits du dossier, de la fiche et de l'archivage")
    void approbation() {
        UUID liaison = UUID.randomUUID();
        ApprobationSecurite a = new ApprobationSecurite(liaison, "I", "PAIEMENT_MOBILE", true, List.of(TypeAttestation.ISO_27001), Set.of());
        assertThat(a.getId()).isNotNull();
        assertThat(a.getLiaisonId()).isEqualTo(liaison);
        assertThat(a.getTypeProjet()).isEqualTo("PAIEMENT_MOBILE");
        assertThat(a.getEtat()).isEqualTo(EtatApprobation.EN_ATTENTE_DOSSIER);
        assertThat(a.attestation(TypeAttestation.GSMA)).isEmpty();
        assertThat(a.getVisas()).extracting(VisaSecurite::getNature).containsExactly(NatureVisa.CONFORMITE, NatureVisa.DIRECTION_GENERALE);
        VisaSecurite v = a.visa(NatureVisa.CONFORMITE).orElseThrow();
        assertThat(v.getId()).isNotNull();
        assertThat(v.isActif()).isFalse();
        assertThat(v.getEtat()).isEqualTo(EtatVisa.ATTENDU);
        UUID sig = UUID.randomUUID();
        v.signer(false, "Non", sig);
        assertThat(v.getEtat()).isEqualTo(EtatVisa.SIGNE_DEFAVORABLE);
        assertThat(v.getMotif()).isEqualTo("Non");
        assertThat(v.getSignatureId()).isEqualTo(sig);
        a.constituerDossier("doc", "d.pdf", "e", T, "u", "Prod");
        assertThat(a.getDossierDeposeParId()).isEqualTo("u");
        a.lireFiche(EtatFicheChangement.EN_EXAMEN, T);
        assertThat(a.getFicheEtat()).isEqualTo(EtatFicheChangement.EN_EXAMEN);
        AttestationSecurite at = a.attestation(TypeAttestation.ISO_27001).orElseThrow();
        assertThat(at.getId()).isNotNull();
        assertThat(at.isRequise()).isTrue();
        at.deposer("doc", "iso.pdf", "e", T, "Corr");
        at.verifierConforme(T, "r", "RSSI");
        assertThat(at.getVerifieeParId()).isEqualTo("r");
        a.archiver(T);
        assertThat(a.getArchiveeLe()).isEqualTo(T);
        assertThat(at.getArchiveeLe()).isEqualTo(T);
        at.rejeter("Faux");
        assertThat(at.estFournie()).isFalse();
        assertThat(at.getMotifRejet()).isEqualTo("Faux");
        assertThat(at.getVerifieeParNom()).isNull();
    }
}
