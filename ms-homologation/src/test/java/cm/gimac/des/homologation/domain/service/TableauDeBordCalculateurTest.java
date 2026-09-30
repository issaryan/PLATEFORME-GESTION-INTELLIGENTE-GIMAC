package cm.gimac.des.homologation.domain.service;

import static cm.gimac.des.homologation.domain.parcours.ParcoursSamples.avancer;
import static cm.gimac.des.homologation.domain.parcours.ParcoursSamples.parcours;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.Acteur;
import cm.gimac.des.homologation.domain.parcours.EtapeCode;
import cm.gimac.des.homologation.domain.parcours.EtatApprobation;
import cm.gimac.des.homologation.domain.parcours.EtatProcesVerbal;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.ParcoursSamples;
import cm.gimac.des.homologation.domain.parcours.Personne;
import cm.gimac.des.homologation.domain.parcours.Qualite;
import cm.gimac.des.homologation.domain.parcours.StatutParcours;
import cm.gimac.des.homologation.domain.parcours.TypeAttestation;
import cm.gimac.des.homologation.domain.parcours.Vues;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tableaux de bord par fonction et pilotage : compteurs, liens, tâches, cloisonnement du participant. */
class TableauDeBordCalculateurTest {

    static final LocalDate J = LocalDate.parse("2026-09-01");
    static final Acteur CONDUCTEUR = new Acteur(ParcoursSamples.CONDUCTEUR, "Conducteur", Set.of("HOM_STEP_MANAGE"), null);

    final TableauDeBordCalculateur calcul = new TableauDeBordCalculateur(new ParcoursProjection());

    Parcours cadrage;
    Parcours ateliers;
    Parcours essais;
    Parcours constat;
    Parcours pv;
    Parcours approbation;
    Parcours production;
    Parcours autre;
    List<Parcours> tous;

    @BeforeEach
    void init() {
        cadrage = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        cadrage.etat().setConducteurADesigner(true);
        ateliers = parcours(TypeProjet.PAIEMENT_MOBILE, "REMITTANCE");
        avancer(ateliers, "7", J);
        essais = parcours(TypeProjet.PAIEMENT_MOBILE, "REMITTANCE", "FACTURE");
        avancer(essais, "15", J);
        essais.declarerBlocage("Élément à renouveler", "Relancer", new Personne("u", "n", Fonction.POLE_RESEAU), J.plusDays(3), null, J.minusDays(9));
        constat = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        avancer(constat, "16", J);
        pv = parcours(TypeProjet.DELEGATAIRE, "RETRAIT");
        avancer(pv, "17", J);
        pv.pvEtabli(null, J);
        approbation = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        approbation.etat().fintech(true);
        avancer(approbation, "19", J);
        approbation.pvSigne(true, true, null, J);
        approbation.visas(List.of("CONFORMITE", "DIRECTION_GENERALE"));
        production = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT", "PAIEMENT");
        avancer(production, "31", J);
        production.remettreFiche();
        production.renseignerFiche(List.of(new Parcours.LigneFiche("PAIEMENT", true, null), new Parcours.LigneFiche("RETRAIT", true, null)), null, J);
        autre = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        autre.liaison().chefDeProjetId(UUID.randomUUID());
        autre.etat().setStatut(StatutParcours.CLOSE_SANS_SUITE);
        tous = List.of(cadrage, ateliers, essais, constat, pv, approbation, production, autre);
    }

    Map<String, Vues.Compteur> compteurs(List<Fonction> f) {
        return calcul.tableau(tous, f, CONDUCTEUR, null, Map.of(), J).compteurs().stream().collect(Collectors.toMap(Vues.Compteur::cle, c -> c, (a, b) -> a));
    }

    @Test
    @DisplayName("Chefs de département et DDRM : conducteurs à désigner, liaisons en amont, ateliers, périmètres, ressources, procès-verbaux à signer")
    void chefs() {
        Map<String, Vues.Compteur> c = compteurs(List.of(Fonction.CHEF_DDRM, Fonction.CHEF_DES));
        assertThat(c.get("conducteursADesigner")).isEqualTo(new Vues.Compteur("conducteursADesigner", 1, "warn", "/des/parcours?sansConducteur=1"));
        assertThat(c.get("liaisonsAmont").valeur()).isEqualTo(2);
        assertThat(c.get("ateliersATenir")).satisfies(x -> {
            assertThat(x.valeur()).isEqualTo(1);
            assertThat(x.lien()).isEqualTo(ParcoursProjection.lienOperation(ateliers, ateliers.operation("8").orElseThrow()));
        });
        assertThat(c.get("perimetresAArreter")).isEqualTo(new Vues.Compteur("perimetresAArreter", 1, "warn", ParcoursProjection.lienOperation(cadrage, cadrage.operation("3").orElseThrow())));
        assertThat(c.get("ressourcesAAffecter")).isEqualTo(new Vues.Compteur("ressourcesAAffecter", 0, "ok", "/des/parcours?etape=CADRAGE,LANCEMENT"));
        assertThat(c.get("enIntegrationTests").valeur()).isEqualTo(1);
        assertThat(c.get("pvASigner")).isEqualTo(new Vues.Compteur("pvASigner", 1, "warn", "/des/parcours/" + pv.id() + "?onglet=decision"));
    }

    @Test
    @DisplayName("Conducteur : ses projets, procès-verbaux à établir ; ses liaisons seulement quand il n'a que cette fonction")
    void conducteur() {
        constat.constaterCouverture(null, J);
        Vues.TableauActeur t = calcul.tableau(tous, List.of(Fonction.CONDUCTEUR), CONDUCTEUR, null, Map.of(ParcoursSamples.CONDUCTEUR, "Conducteur de projet 1"), J);
        Map<String, Long> c = t.compteurs().stream().collect(Collectors.toMap(Vues.Compteur::cle, Vues.Compteur::valeur, (a, b) -> a));
        assertThat(c).containsEntry("mesProjets", 6L).containsEntry("pvAEtablir", 1L);
        assertThat(t.fonction()).isEqualTo(Fonction.CONDUCTEUR);
        assertThat(t.liaisons()).hasSize(6).allMatch(l -> "Conducteur de projet 1".equals(l.conducteurNom()));
        assertThat(t.taches()).extracting(Vues.Tache::rang).contains("18");
        assertThat(calcul.tableau(tous, List.of(Fonction.CONDUCTEUR, Fonction.SNA), CONDUCTEUR, null, Map.of(), J).liaisons()).hasSize(7);
    }

    @Test
    @DisplayName("SNA : essais, documents de paramétrage, couvertures à constater, constats de mise en service")
    void sna() {
        Map<String, Vues.Compteur> c = compteurs(List.of(Fonction.SNA));
        assertThat(c.get("essaisEnCours").valeur()).isEqualTo(1);
        assertThat(c.get("essaisEnCours").lien()).isEqualTo(ParcoursProjection.lienOperation(essais, essais.operation("16").orElseThrow()));
        assertThat(c.get("documentsParametrage")).isEqualTo(new Vues.Compteur("documentsParametrage", 0, "ok", "/des/parcours?etape=INTEGRATION"));
        assertThat(c.get("couverturesAConstater").valeur()).isEqualTo(1);
        assertThat(c.get("constatsMiseEnService")).isEqualTo(new Vues.Compteur("constatsMiseEnService", 1, "warn", "/des/parcours/" + production.id() + "?onglet=mise-en-service"));
    }

    @Test
    @DisplayName("Production monétique, réseau, système, sécurité, conformité, direction générale")
    void fonctionsTechniques() {
        Map<String, Vues.Compteur> pm = compteurs(List.of(Fonction.PRODUCTION_MONETIQUE));
        assertThat(pm.get("dossiersChangement")).isEqualTo(new Vues.Compteur("dossiersChangement", 1, "warn", "/des/parcours/" + approbation.id() + "?onglet=decision"));
        assertThat(pm.get("operationsBranche").valeur()).isZero();
        assertThat(pm.get("rapportsAConsolider").valeur()).isZero();
        Map<String, Vues.Compteur> res = compteurs(List.of(Fonction.POLE_RESEAU));
        assertThat(res.get("liensEssai").valeur()).isZero();
        assertThat(res).containsKey("operationsReseau");
        Map<String, Vues.Compteur> sys = compteurs(List.of(Fonction.POLE_SYSTEME));
        assertThat(sys.get("scriptsDepot").ton()).isEqualTo("ok");
        assertThat(sys.get("operationsSysteme").valeur()).isZero();
        approbation.attestationDeposee(TypeAttestation.PCI_DSS);
        approbation.dossierChangementConstitue(null, J);
        Map<String, Vues.Compteur> rssi = compteurs(List.of(Fonction.RSSI));
        assertThat(rssi.get("attestationsAVerifier").valeur()).isEqualTo(1);
        assertThat(rssi.get("approbationsEnAttente").valeur()).isEqualTo(1);
        assertThat(rssi.get("ceremoniesCles").valeur()).isZero();
        Map<String, Vues.Compteur> conf = compteurs(List.of(Fonction.CONFORMITE));
        assertThat(conf.get("visasAttendus").valeur()).isEqualTo(1);
        assertThat(conf.get("fintechsCobac")).isEqualTo(new Vues.Compteur("fintechsCobac", 1, "info", null));
        assertThat(compteurs(List.of(Fonction.DG)).get("visasAttendus").valeur()).isEqualTo(1);
        assertThat(approbation.etat().getEtatApprobation()).isEqualTo(EtatApprobation.DOSSIER_CONSTITUE);
    }

    @Test
    @DisplayName("Tâches : responsable, validateur ; triées par échéance ; lien vers l'onglet du geste")
    void taches() {
        Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        avancer(p, "27", J);
        Vues.TableauActeur t = calcul.tableau(List.of(p), List.of(Fonction.POLE_SYSTEME), CONDUCTEUR, null, Map.of(), J);
        assertThat(t.taches()).extracting(Vues.Tache::rang).containsExactly("28", "29");
        assertThat(t.taches()).extracting(Vues.Tache::qualite).containsOnly(Qualite.R);
        assertThat(t.taches().get(0).oubliSansSymptome()).isTrue();
        assertThat(t.taches().get(0).actionsPermises()).isNull();
        assertThat(t.compteurs()).filteredOn(c -> c.cle().equals("scriptsDepot")).singleElement().satisfies(c -> assertThat(c.ton()).isEqualTo("crit"));
        p.deposerPreuve(p.operation("28").orElseThrow(), cm.gimac.des.homologation.domain.parcours.Preuve.piece("d", "n", "e", J, "x"), null, null, null);
        Vues.TableauActeur v = calcul.tableau(List.of(p), List.of(Fonction.CONDUCTEUR), CONDUCTEUR, null, Map.of(), J);
        assertThat(v.taches()).extracting(Vues.Tache::qualite).containsExactly(Qualite.V);
        Vues.TableauActeur retard = calcul.tableau(List.of(p), List.of(Fonction.POLE_SYSTEME), CONDUCTEUR, null, Map.of(), J.plusYears(1));
        assertThat(retard.compteurs()).filteredOn(c -> c.cle().equals("tachesEnRetard")).singleElement().satisfies(c -> assertThat(c.valeur()).isEqualTo(2));
        Vues.TableauActeur chefs = calcul.tableau(List.of(parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT")), List.of(Fonction.CHEFS_DEPARTEMENT), CONDUCTEUR, null, Map.of(), J);
        assertThat(chefs.taches()).isEmpty();
        assertThat(calcul.tableau(List.of(), List.of(), CONDUCTEUR, null, Map.of(), J).fonction()).isEqualTo(Fonction.DDRM);
    }

    @Test
    @DisplayName("Participant : cloisonné à son institution, geste et écran de chaque tâche, aucun nom interne")
    void participant() {
        String inst = essais.liaison().getInstitutionId().toString();
        Vues.TableauActeur t = calcul.tableau(tous, List.of(Fonction.PARTICIPANT), new Acteur(null, null, Set.of(), inst), inst, Map.of(), J);
        assertThat(t.liaisons()).hasSize(1).allMatch(l -> "Conducteur de projet GIMAC".equals(l.conducteurNom()));
        assertThat(t.taches()).allMatch(x -> x.lien().startsWith("/portail/") && x.actionsPermises() != null);
        assertThat(t.taches()).extracting(Vues.Tache::rang).contains("16");
        assertThat(t.blocages()).hasSize(1).allMatch(b -> b.responsable().nom().equals("POLE_RESEAU") && b.actionsPermises().isEmpty());
        Map<String, Long> c = t.compteurs().stream().collect(Collectors.toMap(Vues.Compteur::cle, Vues.Compteur::valeur));
        assertThat(c).containsKeys("tachesARealiser", "pvASigner", "attestationsAFournir", "fichesSuivi", "tachesEnRetard", "echeancesProches");
        assertThat(c.get("tachesARealiser")).isEqualTo(t.taches().size());
        String instPv = approbation.liaison().getInstitutionId().toString();
        Vues.TableauActeur a = calcul.tableau(tous, List.of(Fonction.PARTICIPANT), new Acteur(null, null, Set.of(), instPv), instPv, Map.of(), J);
        assertThat(a.compteurs()).filteredOn(x -> x.cle().equals("attestationsAFournir")).singleElement().satisfies(x -> assertThat(x.valeur()).isEqualTo(2));
        pv.pvSigne(true, false, null, J);
        String instSig = pv.liaison().getInstitutionId().toString();
        Vues.TableauActeur s = calcul.tableau(tous, List.of(Fonction.PARTICIPANT), new Acteur(null, null, Set.of(), instSig), instSig, Map.of(), J);
        assertThat(s.compteurs()).filteredOn(x -> x.cle().equals("pvASigner")).singleElement().satisfies(x -> assertThat(x.valeur()).isEqualTo(1));
        assertThat(s.taches()).filteredOn(x -> x.rang().equals("19")).singleElement().satisfies(x -> assertThat(x.actionsPermises()).containsExactly("SIGNER_PROCES_VERBAL"));
        assertThat(pv.etat().getEtatPv()).isEqualTo(EtatProcesVerbal.SIGNE_GIMAC);
    }

    @Test
    @DisplayName("Pilotage : quatre compteurs, répartition par étape hors étapes 1 et 2, délais, six questions")
    void pilotage() {
        Vues.Pilotage p = calcul.pilotage(tous, J);
        assertThat(p.projetsActifs()).isEqualTo(6);
        assertThat(p.projetsBloques()).isEqualTo(1);
        assertThat(p.repartitionParEtape()).extracting(Vues.RepartitionEtape::etape).doesNotContain(EtapeCode.PRISE_CONTACT, EtapeCode.ADHESION).hasSize(9);
        assertThat(p.repartitionParEtape()).filteredOn(r -> r.etape() == EtapeCode.CADRAGE).singleElement().satisfies(r -> assertThat(r.nombre()).isEqualTo(1));
        assertThat(p.delaiMoyenCertificationJours()).isNotNull();
        assertThat(p.tauxRespectDelais()).isEqualTo(100);
        assertThat(p.questions().projetsBloques()).singleElement().satisfies(b -> {
            assertThat(b.liaisonId()).isEqualTo(essais.id().toString());
            assertThat(b.jours()).isEqualTo(9);
        });
        assertThat(p.questions().enCertification()).isEqualTo(3);
        assertThat(p.questions().servicesPlusDemandes().get(0)).isEqualTo(new Vues.ServiceNombre("RETRAIT", "Retrait", 5));
        assertThat(p.questions().servicesPlusLongs()).isNotEmpty();
        assertThat(p.questions().delaiMoyenAdhesionProductionJours()).isNotNull();
        assertThat(p.questions().lancementsParService12Mois()).isNotEmpty();
        Vues.Pilotage vide = calcul.pilotage(List.of(), J);
        assertThat(vide.delaiMoyenCertificationJours()).isNull();
        assertThat(vide.tauxRespectDelais()).isNull();
        assertThat(vide.questions().servicesPlusLongs()).isEmpty();
        assertThat(calcul.pilotage(List.of(cadrage), J).echeancesProches()).isEqualTo(1);
    }
}
