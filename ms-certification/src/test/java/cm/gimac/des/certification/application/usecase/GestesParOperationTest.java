package cm.gimac.des.certification.application.usecase;

import static cm.gimac.des.certification.application.usecase.BancDecision.INSTITUTION;
import static cm.gimac.des.certification.application.usecase.BancDecision.aAffecter;
import static cm.gimac.des.certification.application.usecase.BancDecision.chefDes;
import static cm.gimac.des.certification.application.usecase.BancDecision.conducteur;
import static cm.gimac.des.certification.application.usecase.BancDecision.conformite;
import static cm.gimac.des.certification.application.usecase.BancDecision.correspondant;
import static cm.gimac.des.certification.application.usecase.BancDecision.institution;
import static cm.gimac.des.certification.application.usecase.BancDecision.interne;
import static cm.gimac.des.certification.application.usecase.BancDecision.pdf;
import static cm.gimac.des.certification.application.usecase.BancDecision.production;
import static cm.gimac.des.certification.application.usecase.ProcesVerbalServiceTest.refus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.certification.application.port.in.Acteur;
import cm.gimac.des.certification.application.port.out.DocumentPort.Fichier;
import cm.gimac.des.certification.application.port.out.ReauthentificationPort;
import cm.gimac.des.certification.application.usecase.DecisionVues.ProcesVerbalVue;
import cm.gimac.des.certification.domain.CertificationException;
import cm.gimac.des.certification.domain.decision.EtapeSignature;
import cm.gimac.des.certification.domain.enumeration.NatureVisa;
import cm.gimac.des.certification.domain.enumeration.ObjetSignature;
import cm.gimac.des.certification.domain.operation.EtatOperation;
import cm.gimac.des.certification.domain.operation.GesteCertification;
import cm.gimac.des.certification.domain.operation.OperationLiaison;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * D-93 §4 et D-94 : gestes de certification soumis à l'opération du parcours qui les porte (projection des événements
 * de MS-HOMOLOGATION) et circuit de signature lu dans la configuration, sans qualité codée.
 */
class GestesParOperationTest {

    final BancDecision banc = new BancDecision(Set.of("DELEGATAIRE"), Set.of("DELEGATAIRE"));

    /** Conducteur de la liaison selon le contexte servi par MS-HOMOLOGATION (c-1 dans le banc). */
    static Acteur conducteurLiaison() {
        return new Acteur("c-1", "Conducteur", Set.of("CER_DOSSIER_READ", "CER_PV_ESTABLISH", "CER_QUITUS_TECHNIQUE_SIGN"), null);
    }

    static Acteur agent(String id, String... permissions) {
        return new Acteur(id, id, Set.of(permissions), null);
    }

    UUID liaison() {
        return banc.liaison("CARTE_INTERBANCAIRE", false, 100, true).liaisonId();
    }

    @Test
    @DisplayName("Liaison sans projection : geste admis (transitoire) ; mode REFUSER : 409 CER_OPERATION_INCONNUE")
    void sansProjection() {
        UUID id = liaison();
        assertThat(banc.operations.exiger(id, GesteCertification.ETABLIR_PV, conducteur(), () -> "c-1")).isEmpty();
        OperationsLiaison strict = new OperationsLiaison(banc.operationsRepo, new ParametresOperations("REFUSER"));
        refus(() -> strict.exiger(id, GesteCertification.ETABLIR_PV, conducteur(), () -> "c-1"), 409, "CER_OPERATION_INCONNUE");
        assertThatThrownBy(() -> new ParametresOperations("PEUT_ETRE")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Version sans opération portant le geste : 409 CER_OPERATION_ABSENTE (details.declencheur)")
    void operationAbsente() {
        UUID id = liaison();
        banc.operationOuverte(id, "3", null, interne("R", "DDRM", "u-x"));
        assertThatThrownBy(() -> banc.procesVerbaux.etablir(id, conducteur())).isInstanceOfSatisfying(CertificationException.class, e -> {
            assertThat(e.code()).isEqualTo("CER_OPERATION_ABSENTE");
            assertThat(e.status()).isEqualTo(409);
            assertThat(e.details()).containsEntry("declencheur", GesteCertification.Declencheur.PV_ETABLI);
        });
        assertThat(banc.pvParLiaison.get(id)).isNull();
    }

    @Test
    @DisplayName("Opération non ouverte (en validation, faite) : 409 CER_OPERATION_NON_OUVERTE ; rouverte par un renvoi")
    void operationNonOuverte() {
        UUID id = liaison();
        UUID op = banc.operationOuverte(id, "18", null, interne("R", "CONDUCTEUR", "u-conducteur"));
        banc.horloge.avancer(Duration.ofSeconds(1));
        Instant validation = banc.horloge.instant();
        banc.operations.enValidation(recue(id, op, "18", validation));
        assertThatThrownBy(() -> banc.procesVerbaux.etablir(id, conducteur())).isInstanceOfSatisfying(CertificationException.class, e -> {
            assertThat(e.code()).isEqualTo("CER_OPERATION_NON_OUVERTE");
            assertThat(e.details().get("operations")).isEqualTo(List.of(Map.of("code", "18", "etat", "EN_VALIDATION")));
        });
        // Événement rejoué plus ancien : ignoré.
        banc.operations.activee(recue(id, op, "18", validation.minusSeconds(5)));
        assertThat(banc.operationsParId.get(op).getEtat()).isEqualTo(EtatOperation.EN_VALIDATION);
        // Renvoi du validateur : l'opération est rouverte.
        banc.operations.activee(recue(id, op, "18", validation.plusSeconds(5)));
        assertThat(banc.procesVerbaux.etablir(id, conducteur()).etat()).isEqualTo("ETABLI");
    }

    @Test
    @DisplayName("Jalon atteint ou dernière signature D-91 : opération faite ; signature D-91 restante : en signature")
    void clotureParEvenements() {
        UUID id = liaison();
        UUID op = banc.operationOuverte(id, "19", null, interne("R", "CHEF_DES", "u-chef-des"));
        banc.operations.signee(id, op, "19", "Signature", false, banc.horloge.instant().plusSeconds(1));
        assertThat(banc.operationsParId.get(op).getEtat()).isEqualTo(EtatOperation.EN_SIGNATURE);
        banc.operations.jalonAtteint(id, op, "19", "Signature", "CERTIFIEE", banc.horloge.instant().plusSeconds(2));
        OperationLiaison o = banc.operationsParId.get(op);
        assertThat(o.getEtat()).isEqualTo(EtatOperation.FAITE);
        assertThat(o.getDeclencheur()).isEqualTo(GesteCertification.Declencheur.PV_SIGNE);
        UUID inconnue = UUID.randomUUID();
        banc.operations.jalonAtteint(id, inconnue, "35", "Mise en production", "EN_PRODUCTION", banc.horloge.instant().plusSeconds(3));
        assertThat(banc.operationsParId.get(inconnue).getEtat()).isEqualTo(EtatOperation.FAITE);
        assertThat(banc.operationsParId.get(inconnue).getJalon()).isEqualTo("EN_PRODUCTION");
        banc.operations.signee(id, null, "19", null, true, null);
    }

    @Test
    @DisplayName("operation-closed.v1 : faite ou sans objet, déclencheur publié retenu, ordre respecté ; déclencheur absent : aucun")
    void clotureEtDeclencheurPublie() {
        UUID id = liaison();
        UUID op = banc.operationOuverte(id, "18", null, interne("R", "CONDUCTEUR", "u-conducteur"));
        Instant t = banc.horloge.instant();
        banc.operations.cloturee(id, op, "FAITE", new OperationLiaison.Description("18", null, null, null, null, null), t.plusSeconds(10));
        assertThat(banc.operationsParId.get(op).getEtat()).isEqualTo(EtatOperation.FAITE);
        assertThat(banc.operationsParId.get(op).getDeclencheur()).isEqualTo(GesteCertification.Declencheur.PV_ETABLI);
        refus(() -> banc.procesVerbaux.etablir(id, conducteur()), 409, "CER_OPERATION_NON_OUVERTE");
        // Réouverture plus ancienne que la clôture : ignorée ; plus récente : rouverte.
        banc.operations.activee(recue(id, op, "18", t.plusSeconds(5)));
        assertThat(banc.operationsParId.get(op).getEtat()).isEqualTo(EtatOperation.FAITE);
        banc.operations.activee(recue(id, op, "18", t.plusSeconds(20)));
        assertThat(banc.operationsParId.get(op).getEtat()).isEqualTo(EtatOperation.OUVERTE);
        // Sans objet (migration) ; état inattendu tenu pour fait.
        UUID sansObjet = UUID.randomUUID();
        banc.operations.cloturee(id, sansObjet, "SANS_OBJET", new OperationLiaison.Description("20", "Dossier", null, null, null, GesteCertification.Declencheur.DOSSIER_CHANGEMENT), t.plusSeconds(30));
        assertThat(banc.operationsParId.get(sansObjet).getEtat()).isEqualTo(EtatOperation.SANS_OBJET);
        assertThat(banc.operationsParId.get(sansObjet).getDeclencheur()).isEqualTo(GesteCertification.Declencheur.DOSSIER_CHANGEMENT);
        banc.operations.cloturee(id, sansObjet, "AUTRE", new OperationLiaison.Description(null, null, null, null, "CERTIFIEE", null), t.plusSeconds(40));
        assertThat(banc.operationsParId.get(sansObjet).getEtat()).isEqualTo(EtatOperation.FAITE);
        assertThat(banc.operationsParId.get(sansObjet).getJalon()).isEqualTo("CERTIFIEE");
        banc.operations.cloturee(id, sansObjet, "FAITE", new OperationLiaison.Description(null, null, null, null, null, null), t);
        // Opération publiée sans déclencheur : le déclencheur antérieur ne survit pas, le code n'en donne aucun.
        UUID autre = liaison();
        UUID op19 = banc.operationOuverte(autre, "19", null, interne("R", "CHEF_DES", "u-chef-des"));
        banc.operations.activee(
            new OperationsLiaison.OperationRecue(
                autre,
                op19,
                banc.horloge.instant().plusSeconds(60),
                new OperationLiaison.Description("19", "Renommée", null, null, null, null),
                List.of(),
                List.of(),
                List.of()
            )
        );
        assertThat(banc.operationsParId.get(op19).getDeclencheur()).isNull();
        UUID sansCorrespondance = liaison();
        banc.operationOuverte(sansCorrespondance, "26", null, interne("R", "CONDUCTEUR", "u-conducteur"));
        refus(() -> banc.procesVerbaux.etablir(sansCorrespondance, conducteur()), 409, "CER_OPERATION_ABSENTE");
    }

    @Test
    @DisplayName("Acteur : désigné (R ou V) ou conducteur admis ; autre : 403 CER_ACTEUR_NON_DESIGNE ; fonction à affecter : porteur de la permission")
    void acteurDesigne() {
        UUID id = liaison();
        banc.operationOuverte(id, "18", null, interne("R", "CONDUCTEUR", "u-designe"), interne("V", "CHEF_DES", "u-chef-des"));
        Acteur intrus = agent("u-intrus", "CER_PV_ESTABLISH");
        assertThatThrownBy(() -> banc.procesVerbaux.etablir(id, intrus)).isInstanceOfSatisfying(CertificationException.class, e -> {
            assertThat(e.code()).isEqualTo("CER_ACTEUR_NON_DESIGNE");
            assertThat(e.status()).isEqualTo(403);
            assertThat(e.details()).containsEntry("geste", "ETABLIR_PV");
        });
        assertThat(banc.operations.exiger(id, GesteCertification.ETABLIR_PV, agent("u-designe"), () -> "c-1")).isPresent();
        assertThat(banc.operations.exiger(id, GesteCertification.ETABLIR_PV, agent("u-chef-des"), () -> "c-1")).isPresent();
        assertThat(banc.procesVerbaux.etablir(id, conducteurLiaison()).etat()).isEqualTo("ETABLI");

        UUID autre = liaison();
        banc.operationOuverte(autre, "18", null, aAffecter("R", "CONDUCTEUR"));
        assertThat(banc.procesVerbaux.etablir(autre, intrus).etat()).isEqualTo("ETABLI");
    }

    @Test
    @DisplayName("Portail : dépôt admis pour l'institution qui tient l'opération ; dossier de changement soumis à l'opération")
    void portailEtApprobation() {
        UUID id = banc.pvSigne("CARTE_INTERBANCAIRE", false);
        banc.operationOuverte(id, "21", null, institution("R", "PARTICIPANT", INSTITUTION), interne("V", "RSSI", "u-rssi"));
        Fichier f = new Fichier("pci.pdf", "application/pdf", pdf());
        assertThat(banc.approbations.deposerAttestation(id, "PCI_DSS", f, correspondant(INSTITUTION)).attestations()).isNotEmpty();
        // Dossier de changement : opération 20 absente de la projection de cette liaison.
        refus(
            () -> banc.approbations.constituerDossier(id, UUID.randomUUID(), BancDecision.DOC_SHA, "dossier.pdf", production()),
            409,
            "CER_OPERATION_ABSENTE"
        );
        banc.operationOuverte(id, "20", null, interne("R", "PRODUCTION_MONETIQUE", "u-autre"));
        UUID doc = UUID.randomUUID();
        banc.documents.connaitre(doc, BancDecision.DOC_SHA);
        refus(() -> banc.approbations.constituerDossier(id, doc, BancDecision.DOC_SHA, "dossier.pdf", production()), 403, "CER_ACTEUR_NON_DESIGNE");
        Acteur designe = new Acteur("u-autre", "Production désignée", Set.of("CER_CHANGE_RECORD_LINK"), null);
        assertThat(banc.approbations.constituerDossier(id, doc, BancDecision.DOC_SHA, "dossier.pdf", designe).dossierChangement()).isNotNull();
    }

    @Test
    @DisplayName("D-94 : circuit du procès-verbal lu dans les signatures de l'opération, ordonné, signataire désigné, figé")
    void circuitDeLOperation() {
        UUID id = liaison();
        banc.procesVerbaux.etablir(id, conducteur());
        List<EtapeSignature> configurees = List.of(
            new EtapeSignature("VISA_DDRM", "Visa de la DDRM", "DDRM", 1, false),
            new EtapeSignature("SIGNATURE_DG", "Signature de la DG", "DG", 2, false)
        );
        banc.operationOuverte(id, "19", configurees, interne("R", "DDRM", "u-ddrm"), interne("V", "DG", "u-dg"), institution("R", "PARTICIPANT", INSTITUTION));
        Acteur ddrm = agent("u-ddrm", "CER_DOSSIER_READ", "CER_QUITUS_TECHNIQUE_SIGN");
        Acteur dg = agent("u-dg", "CER_DOSSIER_READ", "CER_QUITUS_TECHNIQUE_SIGN");
        // Le chef du DES ne tient aucune fonction signataire attendue.
        refus(() -> banc.procesVerbaux.signerGimac(id, chefDes(), "a"), 403, "CER_SIGNATAIRE_NON_DESIGNE");
        // La DG signe en second : ordre.
        refus(() -> banc.procesVerbaux.signerGimac(id, dg, "a"), 403, "CER_SIGNATAIRE_NON_DESIGNE");
        ProcesVerbalVue v = banc.procesVerbaux.consulter(id, ddrm);
        assertThat(v.actionsPermises()).contains("SIGNER_GIMAC");
        assertThat(v.circuit()).extracting(DecisionVues.EtapeCircuitVue::code).containsExactly("VISA_DDRM", "SIGNATURE_DG", "PARTICIPANT");
        assertThat(banc.procesVerbaux.consulter(id, dg).actionsPermises()).doesNotContain("SIGNER_GIMAC");
        assertThat(banc.procesVerbaux.signerGimac(id, ddrm, "a").etat()).isEqualTo("ETABLI");
        assertThat(banc.signaturesEnregistrees.get(0).getEtape()).isEqualTo("VISA_DDRM");
        assertThat(banc.signaturesEnregistrees.get(0).getFonction()).isEqualTo("DDRM");
        // Le participant (ordre 1) signe à son tour, avant la DG.
        assertThat(banc.procesVerbaux.signerParticipant(id, correspondant(INSTITUTION), "a").etat()).isEqualTo("SIGNE_PARTICIPANT");
        ProcesVerbalVue signe = banc.procesVerbaux.signerGimac(id, dg, "a");
        assertThat(signe.etat()).isEqualTo("SIGNE");
        assertThat(signe.circuit()).allMatch(e -> "SIGNEE".equals(e.etat()));
        assertThat(banc.pvParLiaison.get(id).getCircuit()).extracting(EtapeSignature::code).containsExactly("VISA_DDRM", "SIGNATURE_DG", "PARTICIPANT");
        assertThat(banc.types()).contains(cm.gimac.des.certification.domain.event.DecisionEvenements.PV_SIGNE);
    }

    @Test
    @DisplayName("D-94 : ordre du circuit par défaut respecté côté portail (409 CER_SIGNATURE_ORDRE) ; conducteur admis à défaut de désigné")
    void ordreEtConducteur() {
        UUID id = liaison();
        banc.procesVerbaux.etablir(id, conducteur());
        banc.operationOuverte(
            id,
            "19",
            List.of(new EtapeSignature("GIMAC", "GIMAC", "CHEF_DES", 1, false), new EtapeSignature("CLOTURE", "Clôture", "DG", 2, false)),
            interne("R", "CHEF_DES", "u-chef-des")
        );
        assertThat(banc.procesVerbaux.signerGimac(id, conducteurLiaison(), "a").etat()).isEqualTo("ETABLI");
        // Reste CLOTURE (ordre 2) côté DES et PARTICIPANT (ordre 1) : la DG attend le participant.
        refus(() -> banc.procesVerbaux.signerGimac(id, conducteurLiaison(), "a"), 409, "CER_SIGNATURE_ORDRE");
    }

    @Test
    @DisplayName("D-94 : visa signé par la fonction du circuit configuré ; autre personne désignée : 403")
    void visaParCircuit() {
        UUID id = banc.pvSigne("DELEGATAIRE", false);
        banc.operationOuverte(id, "23", null, interne("R", "RSSI", "u-rssi"), interne("C", "CONFORMITE", "u-autre-conf"));
        refus(() -> banc.approbations.signerVisa(id, NatureVisa.CONFORMITE, true, null, conformite(), "a"), 403, "CER_SIGNATAIRE_NON_DESIGNE");
        Acteur designee = new Acteur("u-autre-conf", "Conformité désignée", Set.of("CER_QUITUS_CONFORMITE_SIGN"), null);
        banc.approbations.signerVisa(id, NatureVisa.CONFORMITE, true, null, designee, "a");
        assertThat(banc.signaturesEnregistrees.get(banc.signaturesEnregistrees.size() - 1).getEtape()).isEqualTo("CONFORMITE");
        // DG : fonction tenue par personne sur l'opération, laissée au porteur de la permission.
        banc.approbations.signerVisa(id, NatureVisa.DIRECTION_GENERALE, true, null, BancDecision.dg(), "a");
        assertThat(banc.signaturesEnregistrees.get(banc.signaturesEnregistrees.size() - 1).getFonction()).isEqualTo("DG");
    }

    @Test
    @DisplayName("D-94 : réauthentification FIDO2 de finalité SIGNATURE_OPERATION (configurable) ; circuits configurés contrôlés")
    void finaliteEtConfiguration() {
        List<String> finalites = new ArrayList<>();
        ReauthentificationPort capture = (assertion, sujet, finalite) -> {
            finalites.add(finalite);
            return true;
        };
        ScellementSignatures s = new ScellementSignatures(capture, banc.cachet, banc.signatures, banc.jetons, banc.horloge, banc.circuits);
        s.signer(UUID.randomUUID(), ObjetSignature.VISA, banc.circuits.visa(NatureVisa.DIRECTION_GENERALE), BancDecision.dg(), true, null, "e", "a");
        assertThat(finalites).containsExactly("SIGNATURE_OPERATION");
        assertThat(banc.circuits.procesVerbalParDefaut()).extracting(EtapeSignature::fonction).containsExactly("CHEF_DES", "PARTICIPANT");
        var sansPv = new ParametresCircuits(List.of(), BancDecision.circuitsParDefaut().visas(), "AUTRE_FINALITE");
        assertThatThrownBy(() -> new CircuitsSignature(sansPv)).isInstanceOf(IllegalStateException.class);
        var sansVisa = new ParametresCircuits(BancDecision.circuitsParDefaut().procesVerbal(), Map.of(), null);
        assertThatThrownBy(() -> new CircuitsSignature(sansVisa)).isInstanceOf(IllegalStateException.class);
        var doublon = new ParametresCircuits(
            List.of(new ParametresCircuits.Etape("A1", null, "DG", 1, false), new ParametresCircuits.Etape("A1", null, "DG", 2, true)),
            BancDecision.circuitsParDefaut().visas(),
            null
        );
        assertThatThrownBy(() -> new CircuitsSignature(doublon)).isInstanceOf(IllegalStateException.class);
        assertThat(new CircuitsSignature(new ParametresCircuits(BancDecision.circuitsParDefaut().procesVerbal(), BancDecision.circuitsParDefaut().visas(), "X_Y")).finalite()).isEqualTo("X_Y");
    }

    OperationsLiaison.OperationRecue recue(UUID liaison, UUID op, String code, Instant le) {
        return new OperationsLiaison.OperationRecue(
            liaison,
            op,
            le,
            new OperationLiaison.Description(code, "Opération " + code, "EVENEMENT_EXTERNE", "DECISION", null, BancDecision.DECLENCHEURS.get(code)),
            List.of(interne("R", "CONDUCTEUR", "u-conducteur")),
            List.of(),
            null
        );
    }
}
