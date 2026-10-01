package cm.gimac.des.homologation.domain.parcours;

import static cm.gimac.des.homologation.domain.parcours.ParcoursSamples.avancer;
import static cm.gimac.des.homologation.domain.parcours.ParcoursSamples.parcours;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Règles du parcours (étapes 3 à 10), reprises du serveur de substitution de l'interface. */
class ParcoursTest {

    static final LocalDate J = LocalDate.parse("2026-10-06");
    static final Preuve PIECE = Preuve.piece("d1", "piece.pdf", "b".repeat(64), J, "Test");
    static final Acteur CONDUCTEUR = new Acteur(ParcoursSamples.CONDUCTEUR, "Conducteur", Set.of(), null);
    static final Acteur SNA = new Acteur(ParcoursSamples.SNA, "SNA", Set.of(), null);

    static void refus(ThrowingCallable c, String code) {
        assertThatThrownBy(c).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo(code));
    }

    static OperationParcours op(Parcours p, String rang) {
        return p.operation(rang).orElseThrow();
    }

    @Nested
    class Instanciation {

        @ParameterizedTest(name = "{0} : {1} opérations, le rang 27 suit {2}")
        @CsvSource({ "CARTE_INTERBANCAIRE, 43, C10", "PAIEMENT_MOBILE, 49, M17", "DELEGATAIRE, 67, D34" })
        void suiteDuType(TypeProjet type, int nombre, String finBranche) {
            Parcours p = parcours(type, "RETRAIT");
            assertThat(p.operations()).hasSize(nombre);
            assertThat(p.operations().get(0).getRang()).isEqualTo("3");
            assertThat(op(p, "27").getPredecesseurs()).containsExactly(finBranche);
            assertThat(p.operation("1")).isEmpty();
            assertThat(p.operation("5").isPresent()).isEqualTo(type != TypeProjet.PAIEMENT_MOBILE);
            assertThat(op(p, "16").getPlateforme()).isEqualTo(type == TypeProjet.PAIEMENT_MOBILE ? PlateformeExecution.SIMULATEUR : null);
        }

        @Test
        @DisplayName("Ouverture : rang 3 à faire, le reste verrouillé ; échéances planifiées ; qualités par fonction")
        void etatInitial() {
            Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT", "PAIEMENT");
            assertThat(op(p, "3").getEtat()).isEqualTo(EtatOperationParcours.A_FAIRE);
            assertThat(p.operations().stream().filter(o -> o.getEtat() == EtatOperationParcours.A_FAIRE)).hasSize(1);
            assertThat(op(p, "3").getEcheance()).isEqualTo(ParcoursSamples.DEBUT.plusDays(4));
            assertThat(op(p, "4").getEcheance()).isEqualTo(ParcoursSamples.DEBUT.plusDays(7));
            assertThat(p.predecesseursNonFaits(op(p, "30"))).containsExactly("28", "29");
            assertThat(p.etapeCourante()).isEqualTo(EtapeCode.CADRAGE);
            assertThat(p.etat().getStatut()).isEqualTo(StatutParcours.CADREE);
            assertThat(p.liaison().getEtat()).isEqualTo(EtatLiaison.OUVERTE);
            Raci r3 = op(p, "3").getRaci();
            assertThat(r3.r().fonction()).isEqualTo(Fonction.DDRM);
            assertThat(r3.e()).extracting(Personne::fonction).containsExactly(Fonction.PARTICIPANT);
            assertThat(r3.v().fonction()).isEqualTo(Fonction.CONDUCTEUR);
            assertThat(r3.i()).extracting(Personne::fonction).containsExactly(Fonction.CHEF_DDRM);
            assertThat(op(p, "18").getRaci().v().fonction()).isEqualTo(Fonction.CHEF_DES);
            assertThat(op(p, "19").isPivot()).isTrue();
            assertThat(op(p, "28").isOubliSansSymptome()).isTrue();
            assertThat(op(p, "C1").isCle()).isTrue();
            assertThat(op(p, "29").isSecret()).isTrue();
            assertThat(op(p, "12").isValeurReference()).isTrue();
            assertThat(op(p, "15").getGrain()).isEqualTo(GrainPreuve.PHASE);
            assertThat(p.dateCibleProduction()).isEqualTo(op(p, "31").getEcheance());
            assertThat(p.prochaineEcheance()).isEqualTo(op(p, "3").getEcheance());
        }

        @Test
        @DisplayName("Délégataire : personnalisateur externe (E) sur D1 et D2")
        void delegataire() {
            Parcours p = parcours(TypeProjet.DELEGATAIRE, "RETRAIT");
            assertThat(op(p, "D1").getRaci().r().fonction()).isEqualTo(Fonction.CONDUCTEUR);
            assertThat(op(p, "D1").getRaci().e()).extracting(Personne::fonction).containsExactly(Fonction.PERSONNALISATEUR);
            assertThat(op(p, "D1").getRaci().v().fonction()).isEqualTo(Fonction.CHEF_DES);
        }
    }

    @Nested
    class Gestes {

        @Test
        @DisplayName("Livrable : clôt l'opération ouverte et ouvre les suivantes ; refusé ailleurs")
        void livrable() {
            Parcours p = parcours(TypeProjet.PAIEMENT_MOBILE, "PAIEMENT_MARCHAND");
            p.deposerLivrable(op(p, "3"), PIECE, J);
            assertThat(op(p, "3")).satisfies(o -> {
                assertThat(o.getEtat()).isEqualTo(EtatOperationParcours.FAITE);
                assertThat(o.getFaiteLe()).isEqualTo(J);
                assertThat(o.getPreuve().nom()).isEqualTo("piece.pdf");
            });
            assertThat(op(p, "4").getEtat()).isEqualTo(EtatOperationParcours.A_FAIRE);
            assertThat(op(p, "6").getEtat()).isEqualTo(EtatOperationParcours.A_FAIRE);
            refus(() -> p.deposerLivrable(op(p, "3"), PIECE, J), "HOM_OPERATION_NOT_OPEN");
            refus(() -> p.deposerLivrable(op(p, "7"), PIECE, J), "HOM_OPERATION_NOT_OPEN");
            avancer(p, "11", J);
            refus(() -> p.deposerLivrable(op(p, "12"), PIECE, J), "HOM_OPERATION_NOT_OPEN");
        }

        @Test
        @DisplayName("Ressources (rang 6) : ouverture exigée, au moins une, la fonction est remplacée par la personne")
        void ressources() {
            Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
            Personne sna = new Personne(UUID.randomUUID().toString(), "Nouvel ingénieur SNA", Fonction.SNA);
            refus(() -> p.affecterRessources(List.of(sna), PIECE, J), "HOM_STEP_NOT_OPEN");
            p.terminer(op(p, "3"), PIECE, J);
            refus(() -> p.affecterRessources(List.of(), PIECE, J), "HOM_RESOURCES_REQUIRED");
            p.affecterRessources(List.of(sna), PIECE, J);
            assertThat(op(p, "6").faite()).isTrue();
            assertThat(op(p, "10").getRaci().r()).isEqualTo(sna);
            assertThat(op(p, "33").getRaci().r()).isEqualTo(sna);
            assertThat(p.etat().getRessources()).containsEntry(Fonction.SNA, sna);
        }

        @Test
        @DisplayName("Preuve : cérémonie C1 complète et contrôlée, jamais la clé ; validation par V, jamais par R")
        void ceremonie() {
            Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
            avancer(p, "24", J);
            OperationParcours c1 = op(p, "C1");
            assertThat(c1.getEtat()).isEqualTo(EtatOperationParcours.A_FAIRE);
            Ceremonie ok = new Ceremonie("2026-09-28", List.of("RSSI", "Dépositaire 2", "Dépositaire 3"), List.of("a1b2c3", "0F0F0F", "ABCD"));
            refus(() -> p.deposerPreuve(c1, PIECE, null, null, null), "HOM_KEY_CEREMONY_INCOMPLETE");
            refus(() -> p.deposerPreuve(c1, PIECE, null, null, new Ceremonie("2026-09-28", List.of("RSSI", " ", "X"), ok.codesControle())), "HOM_KEY_CEREMONY_INCOMPLETE");
            refus(() -> p.deposerPreuve(c1, PIECE, null, null, new Ceremonie("hier", ok.porteurs(), ok.codesControle())), "HOM_KEY_CEREMONY_INCOMPLETE");
            refus(() -> p.deposerPreuve(c1, PIECE, null, null, new Ceremonie("2026-09-28", ok.porteurs(), List.of("A1B2C3", "zz", "ABCD"))), "HOM_KEY_CHECK_VALUE_INVALID");
            refus(() -> p.deposerPreuve(c1, PIECE, null, "XYZ", ok), "HOM_KEY_CHECK_VALUE_INVALID");
            p.deposerPreuve(c1, PIECE, "valeur ignorée", null, ok);
            assertThat(c1.getEtat()).isEqualTo(EtatOperationParcours.EN_VALIDATION);
            assertThat(c1.getPreuve().ceremonie().codesControle()).containsExactly("A1B2C3", "0F0F0F", "ABCD");
            assertThat(c1.getPreuve().valeurReference()).isNull();
            refus(() -> p.deposerPreuve(c1, PIECE, null, null, ok), "HOM_OPERATION_NOT_OPEN");
            Acteur rssi = new Acteur(null, "RSSI", Set.of(), null);
            p.valider(c1, rssi, J);
            assertThat(c1.faite()).isTrue();
            refus(() -> p.valider(c1, rssi, J), "HOM_OPERATION_NOT_IN_VALIDATION");
        }

        @Test
        @DisplayName("Preuve : code de contrôle exigé pour une clé, trace seule pour un secret, valeur de référence exigée")
        void clesSecretsValeurs() {
            Parcours d = parcours(TypeProjet.DELEGATAIRE, "RETRAIT");
            avancer(d, "D2", J);
            OperationParcours d3 = op(d, "D3");
            refus(() -> d.deposerPreuve(d3, PIECE, null, " ", null), "HOM_KEY_CHECK_VALUE_REQUIRED");
            d.deposerPreuve(d3, PIECE, null, "0a1b", null);
            assertThat(d3.getPreuve().codeControle()).isEqualTo("0A1B");
            Parcours c = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
            avancer(c, "27", J);
            OperationParcours o29 = op(c, "29");
            c.deposerPreuve(o29, PIECE, null, "A1B2C3", null);
            assertThat(o29.getPreuve().codeControle()).isNull();
            assertThat(o29.getPreuve().valeurReference()).isNull();
            Parcours m = parcours(TypeProjet.PAIEMENT_MOBILE, "REMITTANCE");
            avancer(m, "26", J);
            OperationParcours m2 = op(m, "M2");
            refus(() -> m.deposerPreuve(m2, PIECE, " ", null, null), "HOM_REFERENCE_VALUE_REQUIRED");
            m.deposerPreuve(m2, PIECE, " 237001 ", null, null);
            assertThat(m2.getPreuve().valeurReference()).isEqualTo(new ValeurReference("parcours.valeur.M2", "237001"));
            m.valider(m2, SNA, J);
            assertThat(op(m, "M3").getEtat()).isEqualTo(EtatOperationParcours.A_FAIRE);
        }

        @Test
        @DisplayName("Validation refusée au responsable ; qualités R et V distinctes ; C, I, E sans doublon")
        void separation() {
            Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
            OperationParcours o3 = op(p, "3");
            Personne a = new Personne(ParcoursSamples.CONDUCTEUR.toString(), "Conducteur de projet 1", Fonction.CONDUCTEUR);
            p.deposerPreuve(o3, PIECE, null, null, null);
            p.attribuer(o3, Qualite.R, new Personne(ParcoursSamples.SNA.toString(), "SNA", Fonction.SNA));
            refus(() -> p.valider(o3, SNA, J), "HOM_VALIDATOR_IS_RESPONSIBLE");
            refus(() -> p.attribuer(o3, Qualite.V, new Personne(ParcoursSamples.SNA.toString(), "SNA", Fonction.SNA)), "HOM_VALIDATOR_IS_RESPONSIBLE");
            refus(() -> p.attribuer(o3, Qualite.R, a), "HOM_VALIDATOR_IS_RESPONSIBLE");
            p.attribuer(o3, Qualite.C, a);
            p.attribuer(o3, Qualite.C, a);
            p.attribuer(o3, Qualite.I, a);
            p.attribuer(o3, Qualite.E, a);
            p.attribuer(o3, Qualite.V, new Personne("U-CHEF_DDRM", "Chef DDRM", Fonction.CHEF_DDRM));
            assertThat(o3.getRaci().c()).containsExactly(a);
            assertThat(o3.getRaci().i()).contains(a);
            assertThat(o3.getRaci().e()).contains(a);
            p.valider(o3, CONDUCTEUR, J);
            assertThat(o3.faite()).isTrue();
        }

        @Test
        @DisplayName("Blocage daté : les opérations ouvertes sont bloquées, levée qui les rouvre ; levée unique")
        void blocages() {
            Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
            avancer(p, "3", J);
            Personne resp = new Personne("U-DDRM", "Chargé DDRM", Fonction.DDRM);
            BlocageParcours b = p.declarerBlocage(" Pièce manquante ", "Relancer", resp, J.plusDays(4), null, J);
            BlocageParcours b2 = p.declarerBlocage("Second motif", "Attendre", resp, J.plusDays(9), null, J);
            assertThat(b.getMotif()).isEqualTo("Pièce manquante");
            assertThat(b.getEtape()).isEqualTo(EtapeCode.CADRAGE);
            assertThat(b.getResponsable()).isEqualTo(resp);
            assertThat(p.bloquee()).isTrue();
            assertThat(p.etat().isBloquee()).isTrue();
            assertThat(op(p, "4").getEtat()).isEqualTo(EtatOperationParcours.BLOQUEE);
            p.terminer(op(p, "4"), PIECE, J);
            assertThat(op(p, "5").getEtat()).isEqualTo(EtatOperationParcours.BLOQUEE);
            p.leverBlocage(b, "reçu", J);
            assertThat(op(p, "5").getEtat()).isEqualTo(EtatOperationParcours.BLOQUEE);
            refus(() -> p.leverBlocage(b, "encore", J), "HOM_BLOCKAGE_ALREADY_LIFTED");
            p.leverBlocage(b2, null, J);
            assertThat(op(p, "5").getEtat()).isEqualTo(EtatOperationParcours.A_FAIRE);
            assertThat(p.bloquee()).isFalse();
            assertThat(b.getLeveLe()).isEqualTo(J);
            assertThat(b.getCommentaireLevee()).isEqualTo("reçu");
            assertThat(p.blocage(b.getId())).contains(b);
        }

        @Test
        @DisplayName("Coordonnées d'essai (rang 12) : hôte et port contrôlés, rang ouvert exigé, valeur relisible")
        void coordonnees() {
            Parcours p = parcours(TypeProjet.PAIEMENT_MOBILE, "REMITTANCE");
            refus(() -> p.communiquerCoordonnees("hôte invalide!", "8443", null, PIECE, J), "HOM_TECHNICAL_DATA_INVALID");
            refus(() -> p.communiquerCoordonnees("essai.exemple", "99999", null, PIECE, J), "HOM_TECHNICAL_DATA_INVALID");
            refus(() -> p.communiquerCoordonnees("essai.exemple", "8x", null, PIECE, J), "HOM_TECHNICAL_DATA_INVALID");
            refus(() -> p.communiquerCoordonnees(null, null, null, PIECE, J), "HOM_TECHNICAL_DATA_INVALID");
            refus(() -> p.communiquerCoordonnees("essai.exemple", "8443", "x".repeat(501), PIECE, J), "HOM_TECHNICAL_DATA_INVALID");
            refus(() -> p.communiquerCoordonnees("essai.exemple", "8443", null, PIECE, J), "HOM_OPERATION_NOT_OPEN");
            avancer(p, "11", J);
            p.communiquerCoordonnees(" essai.exemple ", "8443", " https://essai.exemple/tx ", PIECE, J);
            assertThat(op(p, "12").getPreuve().valeurReference().valeur()).isEqualTo("essai.exemple:8443");
            assertThat(p.etat().getPointsTerminaison()).isEqualTo("https://essai.exemple/tx");
            assertThat(p.etat().getStatut()).isEqualTo(StatutParcours.EN_INTEGRATION);
        }
    }

    @Nested
    class CouverturesEtStatuts {

        @Test
        @DisplayName("Essais sans mesure reprise : le périmètre inclus est certifié ; constat au rang 17 seulement")
        void certificationSansMesure() {
            Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT", "PAIEMENT");
            refus(() -> p.constaterCouverture(PIECE, J), "HOM_COVERAGE_NOT_READY");
            avancer(p, "14", J);
            assertThat(p.etat().getStatut()).isEqualTo(StatutParcours.EN_INTEGRATION);
            avancer(p, "15", J);
            assertThat(p.etat().getStatut()).isEqualTo(StatutParcours.EN_TEST);
            assertThat(p.services()).allMatch(s -> J.equals(s.getEntreeEssai()));
            assertThat(p.couvertureCertification()).isEqualTo(Couverture.de(0, 2));
            avancer(p, "16", J);
            assertThat(p.couvertureCertification().taux()).isEqualTo(100);
            assertThat(p.liaison().getEtat()).isEqualTo(EtatLiaison.EN_HOMOLOGATION);
            Couverture c = p.constaterCouverture(PIECE, J);
            assertThat(c.complete()).isTrue();
            assertThat(p.etat().isCouvertureConstatee()).isTrue();
            assertThat(p.etat().getCouvertureConstateeLe()).isEqualTo(J);
            assertThat(op(p, "18").getEtat()).isEqualTo(EtatOperationParcours.A_FAIRE);
            assertThat(p.liaison().getEtat()).isEqualTo(EtatLiaison.APTE_CERTIFICATION);
            assertThat(p.servicesCouverts()).containsExactly("PAIEMENT", "RETRAIT");
        }

        @Test
        @DisplayName("Essais mesurés par le simulateur : seul un service à 100 % est certifié")
        void certificationMesuree() {
            Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT", "PAIEMENT", "TRANSFERT", "COMMERCE_ELECTRONIQUE").avecMesures(Map.of("RETRAIT", 100, "PAIEMENT", 100, "TRANSFERT", 100, "COMMERCE_ELECTRONIQUE", 60));
            avancer(p, "16", J);
            Couverture c = p.constaterCouverture(PIECE, J);
            assertThat(c).isEqualTo(new Couverture(75, 3, 4, true, null));
            assertThat(c.complete()).isFalse();
            assertThat(p.mesures()).containsEntry("COMMERCE_ELECTRONIQUE", 60);
            assertThat(Couverture.de(0, 0)).isEqualTo(new Couverture(null, 0, 0, false, "aucun service inclus"));
        }

        @Test
        @DisplayName("Verrous (événements de MS-CERTIFICATION) : rangs 18 à 26, CERTIFIEE puis APPROUVEE_SECURITE")
        void verrous() {
            Parcours p = parcours(TypeProjet.PAIEMENT_MOBILE, "REMITTANCE");
            p.etat().fintech(true);
            avancer(p, "17", J);
            p.pvEtabli(PIECE, J);
            assertThat(p.etat().getEtatPv()).isEqualTo(EtatProcesVerbal.ETABLI);
            assertThat(op(p, "18").faite()).isTrue();
            assertThat(p.attestationsAFournir()).isEmpty();
            p.pvSigne(true, false, PIECE, J);
            assertThat(p.etat().getEtatPv()).isEqualTo(EtatProcesVerbal.SIGNE_GIMAC);
            assertThat(op(p, "19").faite()).isFalse();
            p.pvSigne(false, true, PIECE, J);
            assertThat(p.etat().getEtatPv()).isEqualTo(EtatProcesVerbal.SIGNE);
            assertThat(p.etat().getStatut()).isEqualTo(StatutParcours.CERTIFIEE);
            assertThat(p.liaison().getEtat()).isEqualTo(EtatLiaison.CERTIFIEE);
            assertThat(p.etapeCourante()).isEqualTo(EtapeCode.APPROBATION_SECURITE);
            assertThat(p.attestationsAFournir()).containsExactly(TypeAttestation.ISO_27001, TypeAttestation.GSMA, TypeAttestation.COBAC);
            p.ficheChangementReferencee("CHG-2026-0412", PIECE, J);
            assertThat(op(p, "26").faite()).isFalse();
            assertThat(p.etat().getFicheChangementReference()).isEqualTo("CHG-2026-0412");
            p.dossierChangementConstitue(PIECE, J);
            assertThat(p.etat().getEtatApprobation()).isEqualTo(EtatApprobation.DOSSIER_CONSTITUE);
            assertThat(op(p, "21").getEtat()).isEqualTo(EtatOperationParcours.A_FAIRE);
            p.attestationDeposee(TypeAttestation.GSMA);
            p.attestationDeposee(TypeAttestation.GSMA);
            assertThat(p.etat().getAttestationsDeposees()).containsExactly(TypeAttestation.GSMA);
            assertThat(p.attestationsAFournir()).containsExactly(TypeAttestation.ISO_27001, TypeAttestation.COBAC);
            p.attestationVerifiee(TypeAttestation.GSMA);
            assertThat(p.etat().getEtatApprobation()).isEqualTo(EtatApprobation.EN_VERIFICATION);
            p.visas(List.of("CONFORMITE", "DIRECTION_GENERALE", "CONFORMITE"));
            p.visaSigne("CONFORMITE");
            assertThat(p.etat().getVisasAttendus()).containsExactly("DIRECTION_GENERALE");
            p.attestationsArchivees(PIECE, J);
            assertThat(op(p, "21").faite() && op(p, "22").faite()).isTrue();
            assertThat(p.attestationsAFournir()).isEmpty();
            p.securiteApprouvee(PIECE, J);
            assertThat(p.etat().getStatut()).isEqualTo(StatutParcours.APPROUVEE_SECURITE);
            assertThat(p.liaison().getEtat()).isEqualTo(EtatLiaison.MISE_EN_SERVICE_AUTORISEE);
            // Rang 26 : ouvert par l'approbation, clos aussitôt par la fiche déjà référencée.
            assertThat(op(p, "26").faite()).isTrue();
            assertThat(p.etapeCourante()).isEqualTo(EtapeCode.MISE_EN_PRODUCTION);
            p.ficheChangementReferencee("CHG-2026-0412", PIECE, J);
            assertThat(p.etapeCourante()).isEqualTo(EtapeCode.MISE_EN_PRODUCTION);
        }

        @Test
        @DisplayName("Signature du procès-verbal reçue sans établissement préalable : rangs 18 et 19 clos")
        void signatureSansEtablissement() {
            Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
            avancer(p, "17", J);
            p.pvSigne(false, false, PIECE, J);
            assertThat(p.etat().getEtatPv()).isEqualTo(EtatProcesVerbal.NON_ETABLI);
            p.pvSigne(false, true, PIECE, J);
            assertThat(p.etat().getEtatPv()).isEqualTo(EtatProcesVerbal.SIGNE_PARTICIPANT);
            p.pvSigne(true, false, PIECE, J);
            assertThat(op(p, "18").faite() && op(p, "19").faite()).isTrue();
        }

        @Test
        @DisplayName("Étape 10 : fiche remise, renseignée par le participant, constatée ; mise en production")
        void ficheDeSuivi() {
            Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT", "PAIEMENT");
            refus(() -> p.constaterMiseEnService(PIECE, J), "HOM_SERVICE_SHEET_INCOMPLETE");
            refus(() -> p.renseignerFiche(List.of(), PIECE, J), "HOM_SERVICE_SHEET_NOT_OPEN");
            avancer(p, "31", J);
            assertThat(p.etat().getStatut()).isEqualTo(StatutParcours.EN_PRODUCTION);
            assertThat(p.liaison().getEtat()).isEqualTo(EtatLiaison.CLOSE);
            p.remettreFiche();
            refus(p::remettreFiche, "HOM_SERVICE_SHEET_ALREADY_SENT");
            refus(() -> p.renseignerFiche(List.of(), PIECE, J), "HOM_SERVICE_SHEET_INCOMPLETE");
            refus(() -> p.renseignerFiche(List.of(new Parcours.LigneFiche("RETRAIT", true, null), new Parcours.LigneFiche("PAIEMENT", null, null)), PIECE, J), "HOM_SERVICE_SHEET_INCOMPLETE");
            p.renseignerFiche(List.of(new Parcours.LigneFiche("RETRAIT", true, " "), new Parcours.LigneFiche("PAIEMENT", false, " Reporté ")), PIECE, J);
            assertThat(p.etat().getFicheEtat()).isEqualTo(EtatFicheSuivi.RENSEIGNEE);
            assertThat(op(p, "32").faite()).isTrue();
            assertThat(p.couvertureMiseEnService()).isEqualTo(new Couverture(50, 1, 2, true, null));
            assertThat(p.couvertureCertification().taux()).isEqualTo(100);
            assertThat(p.services()).extracting(ServiceParcours::getCommentaire).containsExactly("Reporté", null);
            p.constaterMiseEnService(PIECE, J);
            assertThat(p.etat().getFicheEtat()).isEqualTo(EtatFicheSuivi.CONSTATEE);
            assertThat(p.etat().getFicheConstateeLe()).isEqualTo(J);
            assertThat(op(p, "33").faite()).isTrue();
            avancer(p, "35", J);
            assertThat(p.etapeCourante()).isEqualTo(EtapeCode.SUIVI);
            assertThat(p.prochaineEcheance()).isNull();
        }

        @Test
        @DisplayName("SLA : dépassé après l'échéance, proche à cinq jours, sans objet pour une opération faite ou verrouillée")
        void sla() {
            Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
            OperationParcours o3 = op(p, "3");
            assertThat(Parcours.sla(o3, o3.getEcheance().plusDays(1))).isEqualTo(Sla.DEPASSE);
            assertThat(Parcours.sla(o3, o3.getEcheance().minusDays(5))).isEqualTo(Sla.ECHEANCE_PROCHE);
            assertThat(Parcours.sla(o3, o3.getEcheance().minusDays(6))).isEqualTo(Sla.DANS_LES_DELAIS);
            assertThat(Parcours.sla(op(p, "4"), J)).isNull();
            assertThat(p.tachesEnRetard(o3.getEcheance().plusDays(1))).isEqualTo(1);
            o3.setEcheance(null);
            assertThat(Parcours.sla(o3, J)).isNull();
        }

        @Test
        @DisplayName("Statuts suspendu et clos sans suite conservés ; automate technique projeté")
        void statutsConserves() {
            Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
            p.etat().setStatut(StatutParcours.SUSPENDUE);
            p.rafraichir();
            assertThat(p.etat().getStatut()).isEqualTo(StatutParcours.SUSPENDUE);
            assertThat(p.liaison().getEtat()).isEqualTo(EtatLiaison.GELEE);
            p.etat().setStatut(StatutParcours.CLOSE_SANS_SUITE);
            p.rafraichir();
            assertThat(p.liaison().getEtat()).isEqualTo(EtatLiaison.GELEE);
            assertThat(Parcours.etatTechnique(StatutParcours.CLOSE_SANS_SUITE, false)).isEqualTo(EtatLiaison.CLOSE);
            assertThat(Parcours.etatTechnique(StatutParcours.EN_TEST, false)).isEqualTo(EtatLiaison.EN_HOMOLOGATION);
        }

        @Test
        @DisplayName("Conducteur désigné : il tient sa fonction sur les opérations non faites")
        void conducteur() {
            Parcours p = parcours(TypeProjet.DELEGATAIRE, "RETRAIT");
            p.etat().setConducteurADesigner(true);
            Personne nouveau = new Personne(UUID.randomUUID().toString(), "Conducteur 2", Fonction.CONDUCTEUR);
            p.designerConducteur(nouveau);
            assertThat(p.etat().isConducteurADesigner()).isFalse();
            assertThat(op(p, "D1").getRaci().r()).isEqualTo(nouveau);
            assertThat(op(p, "3").getRaci().v()).isEqualTo(nouveau);
        }

        @Test
        @DisplayName("Opération ou blocage inconnus")
        void inconnus() {
            Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
            refus(() -> p.operation(UUID.randomUUID()), "HOM_OPERATION_NOT_FOUND");
            assertThat(p.blocage(UUID.randomUUID())).isEmpty();
            assertThat(p.operationDansEtat("99", EtatOperationParcours.A_FAIRE)).isFalse();
            assertThat(p.id()).isEqualTo(p.liaison().getId());
            assertThat(Parcours.geste(op(p, "17"))).isEqualTo("CONSTATER_COUVERTURE");
            assertThat(Parcours.geste(op(p, "15"))).isEqualTo("DEPOSER_LIVRABLE");
            assertThat(Parcours.geste(op(p, "8"))).isEqualTo("DEPOSER_LIVRABLE");
            assertThat(Parcours.geste(op(p, "12"))).isEqualTo("DEPOSER_PREUVE");
        }
    }

    @Test
    @DisplayName("Valeurs : personnes par fonction, qualités, fonctions des profils, attestations exigées, acteur")
    void valeurs() {
        assertThat(Personne.aAffecter(Fonction.RSSI).affectee()).isFalse();
        assertThat(new Personne("u", "Nom", Fonction.RSSI).parFonction()).isEqualTo(new Personne("RSSI", "RSSI", Fonction.RSSI));
        Personne corr = Personne.correspondant("i", null);
        assertThat(corr.parFonction()).isSameAs(corr);
        assertThat(corr.nom()).isEqualTo("Correspondant du participant");
        assertThat(Fonction.desProfils(List.of("ROLE_CHEF_DES", "ROLE_INGENIEUR_HOMOLOGATION", "HOM_LIAISON_READ"))).containsExactlyInAnyOrder(Fonction.CHEF_DES, Fonction.SNA);
        assertThat(Fonction.duProfil("ROLE_CORRESPONDANT")).contains(Fonction.PARTICIPANT);
        assertThat(Fonction.PERSONNALISATEUR.externe()).isTrue();
        assertThat(Fonction.BANQUE_PARTENAIRE.profils()).isEmpty();
        assertThat(TypeAttestation.exigees(TypeProjet.CARTE_INTERBANCAIRE, false)).containsExactly(TypeAttestation.PCI_DSS);
        assertThat(TypeAttestation.exigees(TypeProjet.DELEGATAIRE, true)).containsExactly(TypeAttestation.PCI_DSS, TypeAttestation.COBAC);
        Acteur a = new Acteur(UUID.randomUUID(), "A", null, null);
        assertThat(a.peut("X")).isFalse();
        assertThat(a.est(null)).isFalse();
        assertThat(new Acteur(null, null, Set.of(), null).est(corr)).isFalse();
        assertThat(EtapeCode.APPROBATION_SECURITE.rang()).isNull();
        assertThat(EtatOperationParcours.SANS_OBJET.enCours()).isFalse();
        assertThat(new Raci(null, null, null, null, null).remplacer(Fonction.SNA, corr).r()).isNull();
        assertThat(CatalogueServices.libelle("INCONNU")).isEqualTo("INCONNU");
        assertThat(CatalogueServices.libelle("CASH_IN")).isEqualTo("Cash-in agent");
        Preuve pr = Preuve.piece("d", "n", "e", LocalDate.EPOCH, "RSSI nommé").deposeeParFonction("RSSI");
        assertThat(pr.deposePar()).isEqualTo("RSSI");
        ReferentielOperation r = ParcoursSamples.referentiel().get(0);
        assertThat(r.rangNumerique()).isEqualTo(1);
        assertThat(r.isCle() || r.isSecret() || r.isPivot() || r.isOubliSansSymptome() || r.isValeurReference()).isFalse();
        assertThat(new ReferentielOperation.Cle().rang()).isNull();
    }
}
