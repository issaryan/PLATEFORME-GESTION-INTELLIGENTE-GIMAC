package cm.gimac.des.homologation.domain.service;

import static cm.gimac.des.homologation.domain.parcours.ParcoursSamples.avancer;
import static cm.gimac.des.homologation.domain.parcours.ParcoursSamples.parcours;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.Acteur;
import cm.gimac.des.homologation.domain.parcours.EtapeCode;
import cm.gimac.des.homologation.domain.parcours.EtatOperationParcours;
import cm.gimac.des.homologation.domain.parcours.EtatProcesVerbal;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.OperationParcours;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.ParcoursSamples;
import cm.gimac.des.homologation.domain.parcours.Personne;
import cm.gimac.des.homologation.domain.parcours.Preuve;
import cm.gimac.des.homologation.domain.parcours.Sla;
import cm.gimac.des.homologation.domain.parcours.StatutParcours;
import cm.gimac.des.homologation.domain.parcours.Vues;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** Projection du parcours pour le DES et pour le portail : formes, étapes à six éléments, actions permises. */
class ParcoursProjectionTest {

    static final LocalDate J = LocalDate.parse("2026-10-06");
    static final Preuve PIECE = Preuve.piece("d1", "piece.pdf", "b".repeat(64), J, "Test");
    static final Set<String> TOUT = Set.of("HOM_STEP_MANAGE", "HOM_EVIDENCE_SUBMIT", "HOM_OPERATION_VALIDATE", "HOM_OPERATION_ASSIGN", "CER_DOSSIER_VERIFY", "HOM_PROJECT_DESIGNATE", "HOM_LIAISON_OPEN", "HOM_PORTAL_SUBMIT");
    static final Acteur CONDUCTEUR = new Acteur(ParcoursSamples.CONDUCTEUR, "Conducteur de projet 1", TOUT, null);
    static final Acteur LECTEUR = new Acteur(UUID.randomUUID(), "Lecteur", Set.of("HOM_LIAISON_READ"), null);
    static final ParcoursProjection.Contexte CTX = new ParcoursProjection.Contexte(new Personne(ParcoursSamples.CONDUCTEUR.toString(), "Conducteur de projet 1", Fonction.CONDUCTEUR), "Banque B", 2);

    final ParcoursProjection projection = new ParcoursProjection();

    static OperationParcours op(Parcours p, String rang) {
        return p.operation(rang).orElseThrow();
    }

    @Test
    @DisplayName("Actions d'une opération selon son état et la matrice des rôles ; VALIDER jamais pour le responsable")
    void actions() {
        Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        assertThat(projection.actions(op(p, "3"), CONDUCTEUR)).containsExactly("DEPOSER_PREUVE", "ATTRIBUER");
        assertThat(projection.actions(op(p, "3"), LECTEUR)).isEmpty();
        assertThat(projection.actions(op(p, "4"), CONDUCTEUR)).containsExactly("ATTRIBUER");
        avancer(p, "10", J);
        assertThat(projection.actions(op(p, "11"), new Acteur(null, null, Set.of("HOM_STEP_MANAGE"), null))).containsExactly("DEPOSER_LIVRABLE", "ATTRIBUER");
        avancer(p, "16", J);
        assertThat(projection.actions(op(p, "17"), CONDUCTEUR)).containsExactly("CONSTATER_COUVERTURE", "ATTRIBUER");
        assertThat(projection.actions(op(p, "17"), new Acteur(null, null, Set.of("HOM_EVIDENCE_SUBMIT"), null))).isEmpty();
        assertThat(projection.actions(op(p, "3"), CONDUCTEUR)).containsExactly("CONSULTER");
        p.terminer(op(p, "17"), PIECE, J);
        OperationParcours o18 = op(p, "18");
        // Verrou de la décision : aucun dépôt libre proposé (geste dédié), seulement l'attribution.
        assertThat(projection.actions(o18, CONDUCTEUR)).containsExactly("ATTRIBUER");
        o18.setEtat(EtatOperationParcours.EN_VALIDATION);
        assertThat(projection.actions(o18, CONDUCTEUR)).containsExactly("ATTRIBUER");
        assertThat(projection.actions(o18, new Acteur(UUID.randomUUID(), null, Set.of("HOM_OPERATION_VALIDATE"), null))).containsExactly("VALIDER");
        o18.setEtat(EtatOperationParcours.SANS_OBJET);
        assertThat(projection.actions(o18, CONDUCTEUR)).containsExactly("CONSULTER", "ATTRIBUER");
    }

    @Test
    @DisplayName("Opération : forme de l'interface, prédécesseurs non faits, SLA")
    void operation() {
        Parcours p = parcours(TypeProjet.PAIEMENT_MOBILE, "REMITTANCE");
        Vues.Operation o = projection.operation(p, op(p, "3"), CONDUCTEUR, op(p, "3").getEcheance().plusDays(1));
        assertThat(o.rang()).isEqualTo("3");
        assertThat(o.liaisonId()).isEqualTo(p.id().toString());
        assertThat(o.sla()).isEqualTo(Sla.DEPASSE);
        assertThat(o.transitionsPermises()).isEmpty();
        assertThat(projection.operation(p, op(p, "7"), CONDUCTEUR, J).predecesseursNonFaits()).containsExactly("6");
        String json = JsonMapper.builder().build().writeValueAsString(o);
        assertThat(json).contains("\"R\":{").contains("\"V\":{").contains("\"E\":[");
        assertThat(ParcoursProjection.onglet(op(p, "19"))).isEqualTo("decision");
        assertThat(ParcoursProjection.onglet(op(p, "33"))).isEqualTo("mise-en-service");
        assertThat(ParcoursProjection.onglet(op(parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT"), "C1"))).isEqualTo("operations");
        assertThat(ParcoursProjection.lienOperation(p, op(p, "20"))).isEqualTo("/des/parcours/" + p.id() + "?onglet=decision&operation=" + op(p, "20").getId());
    }

    @Test
    @DisplayName("Étapes à six éléments : prise de contact et adhésion closes, étape courante en cours, étapes à venir")
    void etapes() {
        Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        avancer(p, "5", J);
        List<Vues.Etape> e = projection.etapes(p, J);
        assertThat(e).hasSize(11);
        assertThat(e.get(1)).satisfies(x -> {
            assertThat(x.code()).isEqualTo(EtapeCode.ADHESION);
            assertThat(x.etat()).isEqualTo("CLOSE");
            assertThat(x.elements()).containsValues(true).doesNotContainValue(false);
        });
        Vues.Etape cadrage = e.get(2);
        assertThat(cadrage.etat()).isEqualTo("CLOSE");
        assertThat(cadrage.actionsFaites()).isEqualTo(3);
        assertThat(cadrage.elements()).containsEntry("PREUVE", true).containsEntry("DATE", true);
        assertThat(cadrage.closeLe()).isEqualTo(J);
        assertThat(cadrage.sla()).isNull();
        Vues.Etape lancement = e.get(3);
        assertThat(lancement.etat()).isEqualTo("EN_COURS");
        assertThat(lancement.porteurs()).containsExactly(Fonction.CHEF_DES, Fonction.DDRM);
        assertThat(lancement.debut()).isEqualTo(op(p, "6").getEcheance());
        assertThat(lancement.sla()).isNotNull();
        assertThat(e.get(4).etat()).isEqualTo("A_VENIR");
        // D-93 §5 : le livrable de l'étape porte son libellé, son opération et son code, plus de clé parcours.op.<rang>.
        assertThat(e.get(7).livrables()).extracting(Vues.Livrable::operation, Vues.Livrable::code).containsExactly(org.assertj.core.groups.Tuple.tuple("19", "L1"));
        assertThat(e.get(7).livrables().get(0).libelle()).isNotBlank();
        p.declarerBlocage("m", "a", new Personne("u", "n", Fonction.DDRM), J, null, J);
        assertThat(projection.etapes(p, J).get(3).etat()).isEqualTo("BLOQUEE");
        assertThat(projection.etapes(p, op(p, "6").getEcheance().plusDays(1)).get(3).sla()).isEqualTo(Sla.DEPASSE);
    }

    @Test
    @DisplayName("Dossier du DES : périmètre, couvertures, actions selon l'état et la matrice ; service supplémentaire")
    void dossier() {
        Parcours p = parcours(TypeProjet.PAIEMENT_MOBILE, "REMITTANCE", "FACTURE").avecMesures(Map.of("REMITTANCE", 100, "FACTURE", 40));
        avancer(p, "3", J);
        Vues.Dossier d = projection.dossier(p, CTX, CONDUCTEUR, J);
        assertThat(d.reference()).isEqualTo("LIA-2026-001");
        assertThat(d.statut()).isEqualTo(StatutParcours.CADREE);
        assertThat(d.conducteur().nom()).isEqualTo("Conducteur de projet 1");
        assertThat(d.banquePartenaireNom()).isEqualTo("Banque B");
        assertThat(d.versionReferentiel()).isEqualTo(2);
        assertThat(d.actionsPermises()).containsExactly("DECLARER_BLOCAGE", "DESIGNER_CONDUCTEUR", "AFFECTER_RESSOURCES");
        assertThat(projection.dossier(p, CTX, LECTEUR, J).actionsPermises()).isEmpty();
        assertThat(d.perimetre()).extracting(Vues.ServicePerimetre::etat).containsOnly("NON_DEMARRE");
        assertThat(d.coordonneesTechniques()).isNull();
        assertThat(d.interlocuteursParticipant()).hasSize(1);
        avancer(p, "15", J);
        assertThat(projection.perimetre(p)).extracting(Vues.ServicePerimetre::etat).containsExactly("EN_TEST", "EN_TEST");
        assertThat(projection.perimetre(p)).extracting(Vues.ServicePerimetre::avancementSimulateur).containsExactly(40, 100);
        avancer(p, "16", J);
        assertThat(projection.perimetre(p)).extracting(Vues.ServicePerimetre::etat).containsExactly("EN_TEST", "CERTIFIE");
        assertThat(projection.dossier(p, CTX, CONDUCTEUR, J).actionsPermises()).contains("CONSTATER_COUVERTURE");
        avancer(p, "31", J);
        p.remettreFiche();
        p.renseignerFiche(List.of(new Parcours.LigneFiche("FACTURE", false, null), new Parcours.LigneFiche("REMITTANCE", true, null)), PIECE, J);
        assertThat(projection.perimetre(p)).extracting(Vues.ServicePerimetre::etat).containsExactly("NON_OUVERT", "OUVERT");
        d = projection.dossier(p, CTX, CONDUCTEUR, J);
        assertThat(d.actionsPermises()).contains("OUVRIR_SERVICE_SUPPLEMENTAIRE");
        assertThat(d.coordonneesTechniques()).isNull();
        p.etat().setConducteurADesigner(true);
        p.etat().liaisonOrigineId(UUID.randomUUID());
        d = projection.dossier(p, CTX, CONDUCTEUR, J);
        assertThat(d.conducteur()).isNull();
        assertThat(d.liaisonOrigineId()).isNotNull();
        assertThat(d.actionsPermises()).doesNotContain("DECLARER_BLOCAGE");
        Parcours carte = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        carte.liaison().banquePartenaireId(null);
        assertThat(projection.perimetre(carte).get(0).avancementSimulateur()).isNull();
    }

    @Test
    @DisplayName("Portail : aucun nom interne, conducteur « Conducteur de projet GIMAC », coordonnées relisibles, aucune action")
    void dossierPortail() {
        Parcours p = parcours(TypeProjet.PAIEMENT_MOBILE, "REMITTANCE");
        avancer(p, "11", J);
        p.communiquerCoordonnees("essai.exemple", "9443", "https://essai.exemple/tx", PIECE, J);
        p.declarerBlocage("Élément à renouveler", "Obtenir l'élément", new Personne("u-res", "Ingénieur pôle réseau", Fonction.POLE_RESEAU), J.plusDays(3), null, J);
        Vues.Dossier d = projection.dossierPortail(p, CTX, J);
        assertThat(d.conducteur().nom()).isEqualTo("Conducteur de projet GIMAC");
        assertThat(d.blocages().get(0).responsable().nom()).isEqualTo("POLE_RESEAU");
        assertThat(d.blocages().get(0).actionsPermises()).isEmpty();
        assertThat(d.actionsPermises()).isEmpty();
        assertThat(d.coordonneesTechniques()).isEqualTo(new Vues.Coordonnees("essai.exemple", "9443", "https://essai.exemple/tx", J));
        String json = JsonMapper.builder().build().writeValueAsString(d);
        assertThat(json).doesNotContain("Conducteur de projet 1").doesNotContain("Titulaire ").doesNotContain("Ingénieur");
        p.etat().setConducteurADesigner(true);
        assertThat(projection.dossierPortail(p, CTX, J).conducteur()).isNull();
    }

    @Test
    @DisplayName("Coordonnées absentes d'une preuve sans valeur de référence exploitable")
    void coordonneesIllisibles() {
        Parcours p = parcours(TypeProjet.PAIEMENT_MOBILE, "REMITTANCE");
        avancer(p, "12", J);
        assertThat(projection.coordonnees(p)).isNull();
    }

    @Test
    @DisplayName("Fiche de suivi : REMETTRE en production, CONSTATER une fois renseignée, RENSEIGNER sur le portail")
    void fiche() {
        Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        assertThat(projection.fiche(p, CONDUCTEUR, false).actionsPermises()).isEmpty();
        avancer(p, "31", J);
        assertThat(projection.fiche(p, CONDUCTEUR, false).actionsPermises()).containsExactly("REMETTRE");
        p.remettreFiche();
        assertThat(projection.fiche(p, CONDUCTEUR, true).actionsPermises()).containsExactly("RENSEIGNER");
        assertThat(projection.fiche(p, LECTEUR, true).actionsPermises()).isEmpty();
        p.renseignerFiche(List.of(new Parcours.LigneFiche("RETRAIT", true, "ok")), PIECE, J);
        Vues.FicheSuivi f = projection.fiche(p, CONDUCTEUR, false);
        assertThat(f.actionsPermises()).containsExactly("CONSTATER");
        assertThat(f.lignes()).containsExactly(new Vues.LigneFiche("RETRAIT", "Retrait", true, "ok"));
        assertThat(f.renseigneeLe()).isEqualTo(J);
    }

    @Test
    @DisplayName("Résumé et contexte de certification")
    void resumeEtContexte() {
        Parcours p = parcours(TypeProjet.DELEGATAIRE, "RETRAIT");
        avancer(p, "17", J);
        Vues.LiaisonResume r = projection.resume(p, "Conducteur de projet 1", J);
        assertThat(r.statut()).isEqualTo(StatutParcours.EN_TEST);
        assertThat(r.etapeCourante()).isEqualTo(EtapeCode.CERTIFICATION);
        assertThat(r.conducteurNom()).isEqualTo("Conducteur de projet 1");
        Vues.ContexteCertification c = projection.contexte(p, "Conducteur de projet 1");
        assertThat(c.couvertureCertification()).isEqualTo(new Vues.CouvertureSimple(100, 1, 1));
        assertThat(c.servicesCouverts()).containsExactly("RETRAIT");
        assertThat(c.conducteurId()).isEqualTo(ParcoursSamples.CONDUCTEUR);
        p.etat().setConducteurADesigner(true);
        assertThat(projection.resume(p, "x", J).conducteurNom()).isNull();
    }

    @Test
    @DisplayName("Portail : geste du participant et écran de chaque tâche")
    void gestes() {
        Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        String base = "/portail/liaisons/" + p.id();
        assertThat(projection.gesteParticipant(p, op(p, "3"))).isEqualTo(new ParcoursProjection.GesteParticipant(base, List.of()));
        avancer(p, "7", J);
        assertThat(projection.gesteParticipant(p, op(p, "8")).actionsPermises()).containsExactly("DEPOSER_LIVRABLE");
        assertThat(projection.gesteParticipant(p, op(p, "8")).lien()).isEqualTo(base + "/livrables/" + op(p, "8").getId());
        assertThat(projection.gesteParticipant(p, op(p, "12"))).isEqualTo(new ParcoursProjection.GesteParticipant(base + "/coordonnees", List.of()));
        avancer(p, "11", J);
        assertThat(projection.gesteParticipant(p, op(p, "12")).actionsPermises()).containsExactly("COMMUNIQUER_COORDONNEES");
        assertThat(projection.gesteParticipant(p, op(p, "19")).actionsPermises()).isEmpty();
        p.etat().setEtatPv(EtatProcesVerbal.SIGNE_GIMAC);
        assertThat(projection.gesteParticipant(p, op(p, "19"))).isEqualTo(new ParcoursProjection.GesteParticipant(base + "/proces-verbal", List.of("SIGNER_PROCES_VERBAL")));
        assertThat(projection.gesteParticipant(p, op(p, "21")).lien()).isEqualTo(base + "/attestations");
        avancer(p, "20", J);
        p.etat().setEtatPv(EtatProcesVerbal.SIGNE);
        assertThat(projection.gesteParticipant(p, op(p, "22")).actionsPermises()).containsExactly("DEPOSER_ATTESTATION");
        assertThat(projection.gesteParticipant(p, op(p, "32"))).isEqualTo(new ParcoursProjection.GesteParticipant(base + "/fiche-suivi", List.of()));
        avancer(p, "31", J);
        p.remettreFiche();
        assertThat(projection.gesteParticipant(p, op(p, "32")).actionsPermises()).containsExactly("RENSEIGNER_FICHE_SUIVI");
        assertThat(ParcoursProjection.livrableParticipant(op(p, "15"))).isTrue();
        assertThat(ParcoursProjection.livrableParticipant(op(p, "C1"))).isFalse();
        assertThat(ParcoursProjection.livrableParticipant(op(p, "13"))).isFalse();
        assertThat(ParcoursProjection.livrableParticipant(op(p, "30"))).isFalse();
    }

    @Test
    @DisplayName("Blocage : LEVER pour qui conduit l'étape, tant qu'il est actif")
    void blocage() {
        Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        var b = p.declarerBlocage("m", "a", new Personne("u", "n", Fonction.DDRM), J, null, J);
        assertThat(projection.blocage(b, CONDUCTEUR).actionsPermises()).containsExactly("LEVER");
        assertThat(projection.blocage(b, LECTEUR).actionsPermises()).isEmpty();
        p.leverBlocage(b, null, J);
        assertThat(projection.blocage(b, CONDUCTEUR).actionsPermises()).isEmpty();
    }
}
