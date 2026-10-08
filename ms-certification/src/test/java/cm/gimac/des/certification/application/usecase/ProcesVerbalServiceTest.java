package cm.gimac.des.certification.application.usecase;

import static cm.gimac.des.certification.application.usecase.BancDecision.AUTRE;
import static cm.gimac.des.certification.application.usecase.BancDecision.INSTITUTION;
import static cm.gimac.des.certification.application.usecase.BancDecision.chefDes;
import static cm.gimac.des.certification.application.usecase.BancDecision.conducteur;
import static cm.gimac.des.certification.application.usecase.BancDecision.correspondant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.certification.application.port.in.Acteur;
import cm.gimac.des.certification.application.usecase.DecisionVues.ProcesVerbalVue;
import cm.gimac.des.certification.domain.CertificationException;
import cm.gimac.des.certification.domain.decision.SignatureVerrou;
import cm.gimac.des.certification.domain.event.DecisionEvenements;
import cm.gimac.des.certification.infrastructure.client.ReauthentificationStubAdapter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Verrou 1 (D-56) : règles du procès-verbal de certification, reprises du simulateur de l'interface. */
class ProcesVerbalServiceTest {

    final BancDecision banc = new BancDecision();

    static void refus(ThrowingCallable appel, int statut, String code) {
        assertThatThrownBy(appel).isInstanceOfSatisfying(CertificationException.class, e -> {
            assertThat(e.code()).isEqualTo(code);
            assertThat(e.status()).isEqualTo(statut);
        });
    }

    @Test
    @DisplayName("Liaison inconnue de MS-HOMOLOGATION : 404 HOM_LIAISON_NOT_FOUND")
    void liaisonInconnue() {
        UUID id = UUID.randomUUID();
        refus(() -> banc.procesVerbaux.consulter(id, conducteur()), 404, "HOM_LIAISON_NOT_FOUND");
        refus(() -> banc.procesVerbaux.etablir(id, conducteur()), 404, "HOM_LIAISON_NOT_FOUND");
        refus(() -> banc.procesVerbaux.signerGimac(id, chefDes(), "a"), 404, "HOM_LIAISON_NOT_FOUND");
        refus(() -> banc.procesVerbaux.consulterPortail(id, correspondant(INSTITUTION)), 404, "HOM_LIAISON_NOT_FOUND");
    }

    @Test
    @DisplayName("Avant le constat : aucune action ; à 100 % constatés, ETABLIR pour le conducteur seulement")
    void actionsAvantEtablissement() {
        UUID sansConstat = banc.liaison("CARTE_INTERBANCAIRE", false, 100, false).liaisonId();
        ProcesVerbalVue v = banc.procesVerbaux.consulter(sansConstat, conducteur());
        assertThat(v.etat()).isEqualTo("NON_ETABLI");
        assertThat(v.actionsPermises()).isEmpty();
        assertThat(v.couverture().taux()).isEqualTo(100);
        assertThat(v.couverture().calculable()).isTrue();
        UUID pret = banc.liaison("CARTE_INTERBANCAIRE", false, 100, true).liaisonId();
        assertThat(banc.procesVerbaux.consulter(pret, conducteur()).actionsPermises()).containsExactly("ETABLIR");
        assertThat(banc.procesVerbaux.consulter(pret, chefDes()).actionsPermises()).isEmpty();
    }

    @Test
    @DisplayName("Couverture sous 100 % : ETABLIR_BLOQUE et refus CER_COVERAGE_INCOMPLETE")
    void couvertureIncomplete() {
        UUID id = banc.liaison("CARTE_INTERBANCAIRE", false, 75, true).liaisonId();
        assertThat(banc.procesVerbaux.consulter(id, conducteur()).actionsPermises()).containsExactly("ETABLIR_BLOQUE");
        refus(() -> banc.procesVerbaux.etablir(id, conducteur()), 409, "CER_COVERAGE_INCOMPLETE");
        assertThat(banc.evenements).isEmpty();
    }

    @Test
    @DisplayName("100 % sans constat : CER_PV_NOT_READY ; l'événement de couverture établie rend le PV établissable")
    void constatParEvenement() {
        UUID id = banc.liaison("CARTE_INTERBANCAIRE", false, 100, false).liaisonId();
        refus(() -> banc.procesVerbaux.etablir(id, conducteur()), 409, "CER_PV_NOT_READY");
        banc.procesVerbaux.enregistrerCouvertureEtablie(id, 100, List.of("RETRAIT"));
        assertThat(banc.procesVerbaux.consulter(id, conducteur()).actionsPermises()).containsExactly("ETABLIR");
        assertThat(banc.procesVerbaux.etablir(id, conducteur()).etat()).isEqualTo("ETABLI");
        banc.procesVerbaux.enregistrerCouvertureEtablie(id, 50, List.of());
        assertThat(banc.pvParLiaison.get(id).getCouvertureTaux()).isEqualTo(100);
    }

    @Test
    @DisplayName("Établissement : couverture figée, empreinte, événement pv-established avec l'institution ; pas deux fois")
    void etablissement() {
        UUID id = banc.liaison("CARTE_INTERBANCAIRE", false, 100, true).liaisonId();
        ProcesVerbalVue v = banc.procesVerbaux.etablir(id, conducteur());
        assertThat(v.etat()).isEqualTo("ETABLI");
        assertThat(v.etabliPar()).isEqualTo("Aminata Ngono");
        assertThat(v.etabliLe()).hasToString("2026-09-30");
        assertThat(v.empreinte()).matches("[0-9a-f]{64}");
        assertThat(v.servicesCouverts()).containsExactly("RETRAIT", "PAIEMENT");
        assertThat(v.actionsPermises()).isEmpty();
        BancDecision.Evenement e = banc.evenements.get(0);
        assertThat(e.type()).isEqualTo(DecisionEvenements.PV_ETABLI);
        assertThat(e.institution()).isEqualTo(INSTITUTION);
        assertThat(e.charge()).isEqualTo(new DecisionEvenements.ProcesVerbalEtabli(id, "Aminata Ngono", "u-conducteur", v.empreinte()));
        refus(() -> banc.procesVerbaux.etablir(id, conducteur()), 409, "CER_PV_NOT_READY");
        assertThat(banc.procesVerbaux.consulter(id, chefDes()).actionsPermises()).containsExactly("SIGNER_GIMAC");
    }

    @Test
    @DisplayName("Signature GIMAC : assertion exigée (401), vérifiée (403), cachet horodaté, immuable")
    void signatureGimac() {
        UUID id = banc.liaison("CARTE_INTERBANCAIRE", false, 100, true).liaisonId();
        refus(() -> banc.procesVerbaux.signerGimac(id, chefDes(), "a"), 409, "CER_PV_NOT_SIGNABLE");
        banc.procesVerbaux.etablir(id, conducteur());
        refus(() -> banc.procesVerbaux.signerGimac(id, chefDes(), " "), 401, "IAM_REAUTH_REQUIRED");
        refus(() -> banc.procesVerbaux.signerGimac(id, chefDes(), ReauthentificationStubAdapter.ASSERTION_REFUSEE), 403, "CER_REAUTH_INVALID");
        ProcesVerbalVue v = banc.procesVerbaux.signerGimac(id, chefDes(), "assertion-webauthn");
        assertThat(v.etat()).isEqualTo("SIGNE_GIMAC");
        assertThat(v.signatureGimac().signataire()).isEqualTo("Chef du DES");
        assertThat(v.signatureGimac().fonction()).isEqualTo("CHEF_DES");
        assertThat(v.signatureGimac().horodatage()).isEqualTo(banc.horloge.instant());
        assertThat(v.signatureParticipant()).isNull();
        SignatureVerrou s = banc.signaturesEnregistrees.get(0);
        assertThat(s.getEtape()).isEqualTo("GIMAC");
        assertThat(s.getFonction()).isEqualTo("CHEF_DES");
        assertThat(s.getAssertion()).isEqualTo("assertion-webauthn");
        assertThat(s.getEmpreinteObjet()).isEqualTo(v.empreinte());
        assertThat(s.getEmpreinteDecision()).matches("[0-9a-f]{64}");
        assertThat(s.getCachet()).isNotBlank();
        assertThat(s.getJetonHorodatageId()).isEqualTo(banc.jetonsEnregistres.get(0).getId());
        assertThat(banc.jetonsEnregistres.get(0).getObjetType()).isEqualTo("SIGNATURE");
        assertThat(banc.jetonsEnregistres.get(0).getObjetId()).isEqualTo(s.getId());
        refus(() -> banc.procesVerbaux.signerGimac(id, chefDes(), "a"), 409, "CER_PV_NOT_SIGNABLE");
        assertThat(banc.types()).containsExactly(DecisionEvenements.PV_ETABLI, DecisionEvenements.PV_SIGNATURE_ENREGISTREE);
    }

    @Test
    @DisplayName("Portail : cloisonné à l'institution (404), projection sans nom interne, signature du participant")
    void portail() {
        UUID id = banc.liaison("CARTE_INTERBANCAIRE", false, 100, true).liaisonId();
        Acteur corr = correspondant(INSTITUTION);
        refus(() -> banc.procesVerbaux.consulterPortail(id, correspondant(AUTRE)), 404, "HOM_LIAISON_NOT_FOUND");
        ProcesVerbalVue avant = banc.procesVerbaux.consulterPortail(id, corr);
        assertThat(avant.etat()).isEqualTo("NON_ETABLI");
        assertThat(avant.actionsPermises()).isEmpty();
        refus(() -> banc.procesVerbaux.signerParticipant(id, corr, "a"), 409, "CER_PV_NOT_SIGNABLE");
        refus(() -> banc.procesVerbaux.signerParticipant(id, correspondant(AUTRE), "a"), 404, "HOM_LIAISON_NOT_FOUND");
        banc.procesVerbaux.etablir(id, conducteur());
        banc.procesVerbaux.signerGimac(id, chefDes(), "a");
        refus(() -> banc.procesVerbaux.consulterPortail(id, correspondant(AUTRE)), 404, "HOM_LIAISON_NOT_FOUND");
        refus(() -> banc.procesVerbaux.signerParticipant(id, correspondant(AUTRE), "a"), 404, "HOM_LIAISON_NOT_FOUND");
        refus(() -> banc.procesVerbaux.signerParticipant(id, corr, null), 401, "IAM_REAUTH_REQUIRED");
        ProcesVerbalVue v = banc.procesVerbaux.consulterPortail(id, corr);
        assertThat(v.etabliPar()).isEqualTo("CONDUCTEUR");
        assertThat(v.signatureGimac().signataire()).isEqualTo("CHEF_DES");
        assertThat(v.actionsPermises()).containsExactly("SIGNER_PARTICIPANT");
        Acteur lecteur = new Acteur("u-l", "Lecteur", Set.of("CER_PORTAL_READ"), INSTITUTION);
        assertThat(banc.procesVerbaux.consulterPortail(id, lecteur).actionsPermises()).isEmpty();
        ProcesVerbalVue signe = banc.procesVerbaux.signerParticipant(id, corr, "a");
        assertThat(signe.etat()).isEqualTo("SIGNE");
        assertThat(signe.signatureParticipant().signataire()).isEqualTo("Représentant du participant");
        assertThat(signe.signatureParticipant().fonction()).isEqualTo("PARTICIPANT");
        assertThat(signe.actionsPermises()).isEmpty();
        refus(() -> banc.procesVerbaux.signerParticipant(id, corr, "a"), 409, "CER_PV_NOT_SIGNABLE");
    }

    @Test
    @DisplayName("Double signature dans l'ordre inverse : SIGNE, événement pv-signed et ouverture du verrou 2")
    void doubleSignatureOrdreInverse() {
        UUID id = banc.liaison("PAIEMENT_MOBILE", true, 100, true).liaisonId();
        banc.procesVerbaux.etablir(id, conducteur());
        assertThat(banc.procesVerbaux.signerParticipant(id, correspondant(INSTITUTION), "a").etat()).isEqualTo("SIGNE_PARTICIPANT");
        assertThat(banc.procesVerbaux.consulter(id, chefDes()).actionsPermises()).containsExactly("SIGNER_GIMAC");
        banc.horloge.avancer(java.time.Duration.ofMinutes(5));
        ProcesVerbalVue v = banc.procesVerbaux.signerGimac(id, chefDes(), "a");
        assertThat(v.etat()).isEqualTo("SIGNE");
        assertThat(banc.types()).containsExactly(
            DecisionEvenements.PV_ETABLI,
            DecisionEvenements.PV_SIGNATURE_ENREGISTREE,
            DecisionEvenements.PV_SIGNATURE_ENREGISTREE,
            DecisionEvenements.PV_SIGNE
        );
        var signe = (DecisionEvenements.ProcesVerbalSigne) banc.evenements.get(3).charge();
        assertThat(signe.signatures()).extracting(DecisionEvenements.SignatureResume::fonction).containsExactly("PARTICIPANT", "CHEF_DES");
        assertThat(signe.empreinte()).isEqualTo(v.empreinte());
        assertThat(banc.approParLiaison.get(id).getAttestations()).extracting(a -> a.getType().name()).containsExactly("ISO_27001", "GSMA", "COBAC");
        // L'ouverture est idempotente.
        assertThat(banc.approbations.ouvrir(banc.pvParLiaison.get(id))).isSameAs(banc.approParLiaison.get(id));
    }

    @Test
    @DisplayName("Couverture non calculable (aucun service inclus) : motif affiché, taux absent")
    void couvertureNonCalculable() {
        DecisionVues.CouvertureVue c = DecisionVues.CouvertureVue.de(null, 0, 0);
        assertThat(c.calculable()).isFalse();
        assertThat(c.taux()).isNull();
        assertThat(c.motifNonCalculable()).isEqualTo("aucun service inclus");
        assertThat(DecisionVues.jour(null)).isNull();
    }
}
