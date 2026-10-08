package cm.gimac.des.homologation.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.Declencheurs;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Geste;
import cm.gimac.des.homologation.domain.parcours.GrainPreuve;
import cm.gimac.des.homologation.domain.parcours.Jalon;
import cm.gimac.des.homologation.domain.parcours.NatureLivrable;
import cm.gimac.des.homologation.domain.parcours.OperationParcours;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.ParcoursLiaison;
import cm.gimac.des.homologation.domain.parcours.ParcoursSamples;
import cm.gimac.des.homologation.domain.parcours.Particularite;
import cm.gimac.des.homologation.domain.parcours.Personne;
import cm.gimac.des.homologation.domain.parcours.PlateformeExecution;
import cm.gimac.des.homologation.domain.parcours.Raci;
import cm.gimac.des.homologation.domain.parcours.ReferentielOperation;
import cm.gimac.des.homologation.domain.parcours.SegmentParcours;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Fidélité de la version 1 (D-77, §1 et §3) : pour chaque type de projet, les opérations, prédécesseurs et qualités
 * instanciés depuis la version 1 sont ceux que produisait l'ancien chemin du CSV (algorithme d'instanciation d'avant
 * D-77, reproduit ici à l'identique).
 */
class ConversionAnnexeATest {

    /** Opération telle que l'ancien moteur l'instanciait depuis le CSV. */
    record Ancienne(String rang, EtatAttendu attendu) {}

    record EtatAttendu(
        int ordre,
        Object etape,
        SegmentParcours segment,
        PlateformeExecution plateforme,
        GrainPreuve grain,
        boolean cle,
        boolean secret,
        boolean valeurReference,
        boolean oubli,
        boolean pivot,
        List<String> predecesseurs,
        Raci raci
    ) {}

    /** Ancien chemin (Parcours.instancier avant D-77) : rangs 3 et suivants, branche du type, raccord implicite du rang 27. */
    static List<Ancienne> ancienChemin(TypeProjet type, Parcours.Designation d) {
        List<ReferentielOperation> suite = ConversionAnnexeA.lireCsv()
            .stream()
            .filter(o -> o.getTypes().contains(type))
            .filter(o -> o.rangNumerique() < 0 || o.rangNumerique() >= 3)
            .sorted(Comparator.comparingInt(ReferentielOperation::getOrdre))
            .toList();
        String finBranche = suite.stream().filter(o -> o.getSegment() == SegmentParcours.BRANCHE).reduce((a, b) -> b).map(ReferentielOperation::getRang).orElse(null);
        List<Ancienne> l = new ArrayList<>();
        for (int i = 0; i < suite.size(); i++) {
            ReferentielOperation o = suite.get(i);
            List<String> preds = o.getSegment() == SegmentParcours.AVAL && o.getPredecesseurs().isEmpty() && finBranche != null ? List.of(finBranche) : o.getPredecesseurs();
            PlateformeExecution plateforme = o.getPlateforme();
            if ("15".equals(o.getRang()) || "16".equals(o.getRang())) {
                plateforme = type == TypeProjet.PAIEMENT_MOBILE ? PlateformeExecution.SIMULATEUR : null;
            }
            List<Fonction> porteurs = o.getPorteurs();
            Personne r = d.pour(porteurs.get(0));
            List<Personne> autres = porteurs.stream().skip(1).map(d::pour).toList();
            Personne v = porteurs.get(0) == Fonction.CONDUCTEUR ? d.pour(Fonction.CHEF_DES) : d.pour(Fonction.CONDUCTEUR);
            Raci raci = new Raci(r, autres.stream().filter(p -> !p.fonction().externe()).toList(), v, List.of(d.pour(Fonction.CHEF_DDRM)), autres.stream().filter(p -> p.fonction().externe()).toList());
            l.add(
                new Ancienne(
                    o.getRang(),
                    new EtatAttendu(i, o.getEtape(), o.getSegment(), plateforme, o.getGrain(), o.isCle(), o.isSecret(), o.isValeurReference(), o.isOubliSansSymptome(), o.isPivot(), preds, raci)
                )
            );
        }
        return l;
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "CARTE_INTERBANCAIRE", "PAIEMENT_MOBILE", "DELEGATAIRE" })
    @DisplayName("Version 1 : mêmes opérations, ordre, attributs, prédécesseurs et qualités que l'ancien chemin du CSV")
    void fideliteAuCsv(TypeProjet type) {
        Parcours p = ParcoursSamples.parcours(type, "RETRAIT");
        Liaison l = p.liaison();
        Parcours.Designation d = ParcoursSamples.designation(l);
        List<Ancienne> anciennes = ancienChemin(type, d);
        Set<String> instanciees = anciennes.stream().map(Ancienne::rang).collect(Collectors.toSet());
        assertThat(p.operations()).extracting(OperationParcours::getRang).containsExactlyElementsOf(anciennes.stream().map(Ancienne::rang).toList());
        for (Ancienne a : anciennes) {
            OperationParcours o = p.operation(a.rang()).orElseThrow();
            // Les rangs 1 et 2, tenus par MS-PARTICIPANT, n'étaient jamais instanciés : l'ancien prédécesseur « 2 » du rang 3
            // était réputé fait ; la version 1 ne le porte plus.
            List<String> predsAttendus = a.attendu().predecesseurs().stream().filter(instanciees::contains).toList();
            EtatAttendu obtenu = new EtatAttendu(
                o.getOrdre(),
                o.getEtape(),
                o.getSegment(),
                o.getPlateforme(),
                o.getGrain(),
                o.isCle(),
                o.isSecret(),
                o.isValeurReference(),
                o.isOubliSansSymptome(),
                o.isPivot(),
                o.getPredecesseurs(),
                o.getRaci()
            );
            assertThat(obtenu).as("rang %s", a.rang()).isEqualTo(new EtatAttendu(
                a.attendu().ordre(),
                a.attendu().etape(),
                a.attendu().segment(),
                a.attendu().plateforme(),
                a.attendu().grain(),
                a.attendu().cle(),
                a.attendu().secret(),
                a.attendu().valeurReference(),
                a.attendu().oubli(),
                a.attendu().pivot(),
                predsAttendus,
                a.attendu().raci()
            ));
            assertThat(o.getLibelle()).as("libellé du rang %s", a.rang()).isNotBlank();
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "CARTE_INTERBANCAIRE", "PAIEMENT_MOBILE", "DELEGATAIRE" })
    @DisplayName("Version 1 : complète et conforme au contrôle de publication (graphe et modèle)")
    void publiable(TypeProjet type) {
        ReferentialVersion v = ParcoursSamples.version1(type);
        assertThat(new ReferentialCompletenessChecker().check(v.getOperations())).isEmpty();
        ReferentialGraphValidator.Rapport r = new ReferentialGraphValidator().controler(v.noeuds());
        assertThat(r.anomalies()).isEmpty();
        assertThat(r.ordre()).hasSize(v.getOperations().size());
    }

    @Test
    @DisplayName("Comptes par type : tronc commun (rangs 3 à 35, sans le rang 5 en paiement mobile) et branche")
    void comptes() {
        Map<TypeProjet, Integer> attendus = Map.of(TypeProjet.CARTE_INTERBANCAIRE, 43, TypeProjet.PAIEMENT_MOBILE, 49, TypeProjet.DELEGATAIRE, 67);
        Map<TypeProjet, Integer> branches = Map.of(TypeProjet.CARTE_INTERBANCAIRE, 10, TypeProjet.PAIEMENT_MOBILE, 17, TypeProjet.DELEGATAIRE, 34);
        for (TypeProjet t : TypeProjet.values()) {
            List<ConversionAnnexeA.Operation> ops = ConversionAnnexeA.version1(t);
            assertThat(ops).as("%s", t).hasSize(attendus.get(t));
            assertThat(ops.stream().filter(o -> o.data().segment().parcours() == SegmentParcours.BRANCHE)).as("branche %s", t).hasSize(branches.get(t));
        }
    }

    @Test
    @DisplayName("Gestes, jalons, déclencheurs, particularités et livrables reprennent les rangs autrefois codés en dur")
    void modeleDesRangs() {
        Map<String, ReferenceOperation> c = parCode(TypeProjet.CARTE_INTERBANCAIRE);
        assertThat(c.get("6").getGeste()).isEqualTo(Geste.AFFECTER_RESSOURCES);
        assertThat(c.get("12").getGeste()).isEqualTo(Geste.COORDONNEES_TECHNIQUES);
        assertThat(c.get("12").getValeurReference()).isEqualTo("Adresse et port d'essai");
        assertThat(c.get("17").getGeste()).isEqualTo(Geste.CONSTATER_COUVERTURE);
        assertThat(c.get("17").getJalon()).isEqualTo(Jalon.APTE_CERTIFICATION);
        assertThat(c.get("18").getDeclencheur()).isEqualTo(Declencheurs.PV_ETABLI);
        assertThat(c.get("19").getDeclencheur()).isEqualTo(Declencheurs.PV_SIGNE);
        assertThat(c.get("19").getJalon()).isEqualTo(Jalon.CERTIFIEE);
        assertThat(c.get("20").getDeclencheur()).isEqualTo(Declencheurs.DOSSIER_CHANGEMENT);
        assertThat(List.of(c.get("21").getDeclencheur(), c.get("22").getDeclencheur())).containsOnly(Declencheurs.ATTESTATIONS_ARCHIVEES);
        assertThat(List.of(c.get("23").getDeclencheur(), c.get("24").getDeclencheur(), c.get("25").getDeclencheur())).containsOnly(Declencheurs.SECURITE_APPROUVEE);
        assertThat(c.get("25").getJalon()).isEqualTo(Jalon.APPROUVEE_SECURITE);
        assertThat(c.get("26").getDeclencheur()).isEqualTo(Declencheurs.FICHE_CHANGEMENT);
        assertThat(c.get("26").getGeste()).isEqualTo(Geste.DEPOSER_PREUVE);
        assertThat(c.get("31").getJalon()).isEqualTo(Jalon.EN_PRODUCTION);
        assertThat(List.of(c.get("32").getGeste(), c.get("33").getGeste())).containsOnly(Geste.FICHE_SUIVI);
        assertThat(List.of(c.get("7").getGeste(), c.get("8").getGeste(), c.get("11").getGeste(), c.get("15").getGeste())).containsOnly(Geste.DEPOSER_LIVRABLE);
        assertThat(c.get("16").getGeste()).isEqualTo(Geste.DEPOSER_PREUVE);
        assertThat(c.get("15").getParticularite()).isEqualTo(Particularite.ENTREE_ESSAIS);
        assertThat(c.get("16").getParticularite()).isEqualTo(Particularite.FIN_ESSAIS);
        assertThat(c.get("C1").getParticularite()).isEqualTo(Particularite.CEREMONIE_CLES);
        assertThat(c.get("35").getParticularite()).isEqualTo(Particularite.SERVICE_SUPPLEMENTAIRE);
        assertThat(c.get("27").predecesseursListe()).containsExactly("C10");
        assertThat(c.get("3").predecesseursListe()).isEmpty();
        assertThat(c.get("15").getLivrables()).singleElement().satisfies(x -> {
            assertThat(x.nature()).isEqualTo(NatureLivrable.LIVRABLE);
            assertThat(x.deposant()).isEqualTo(Fonction.SNA);
            assertThat(x.typesPieces()).containsExactly("application/pdf");
            assertThat(x.obligatoire()).isTrue();
        });
        assertThat(c.get("19").getLivrables()).singleElement().extracting(x -> x.deposant()).isEqualTo(Fonction.CHEF_DES);
        assertThat(c.values().stream().filter(o -> !o.getLivrables().isEmpty()).map(ReferenceOperation::getCode)).containsExactlyInAnyOrder("15", "19");
        assertThat(c.get("18").getValidateur()).isEqualTo(Fonction.CHEF_DES);
        assertThat(c.get("D1") == null).isTrue();
        assertThat(parCode(TypeProjet.DELEGATAIRE).get("27").predecesseursListe()).containsExactly("D34");
        assertThat(parCode(TypeProjet.PAIEMENT_MOBILE).get("27").predecesseursListe()).containsExactly("M17");
        assertThat(parCode(TypeProjet.PAIEMENT_MOBILE).get("15").getPlateforme()).isEqualTo(PlateformeExecution.SIMULATEUR);
        assertThat(c.get("15").getPlateforme()).isNull();
        // Attributs historiques dérivés du modèle (complétude HOM-01).
        assertThat(c.get("12").getPartie()).isEqualTo(cm.gimac.des.homologation.domain.enumeration.Partie.PARTICIPANT);
        assertThat(c.get("3").getPhase()).isEqualTo("CADRAGE");
        assertThat(c.get("3").getFonctionResponsable()).isEqualTo("DDRM");
        assertThat(c.get("C1").getPreuveAttendue()).contains("\"cle\":true").contains("SWITCH_INTERBANCAIRE");
    }

    @Test
    @DisplayName("Ancien chemin : le CSV porte les délais et l'ordre lus par la reprise")
    void csv() {
        List<ReferentielOperation> csv = ConversionAnnexeA.lireCsv();
        assertThat(csv).hasSize(96);
        assertThat(csv.stream().mapToInt(ReferentielOperation::getDelaiJours).distinct()).containsExactly(3);
        assertThat(new ParcoursLiaison(java.util.UUID.randomUUID(), "LIA", ParcoursSamples.DEBUT).getVersionCr()).isNull();
    }

    private static Map<String, ReferenceOperation> parCode(TypeProjet t) {
        return ParcoursSamples.version1(t).getOperations().stream().collect(Collectors.toMap(ReferenceOperation::getCode, o -> o));
    }
}
