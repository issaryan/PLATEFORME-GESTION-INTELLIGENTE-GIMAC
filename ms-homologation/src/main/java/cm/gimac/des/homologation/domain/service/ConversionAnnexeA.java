package cm.gimac.des.homologation.domain.service;

import cm.gimac.des.homologation.domain.ReferenceOperationData;
import cm.gimac.des.homologation.domain.enumeration.Segment;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.Declencheurs;
import cm.gimac.des.homologation.domain.parcours.EtapeCode;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Geste;
import cm.gimac.des.homologation.domain.parcours.GrainPreuve;
import cm.gimac.des.homologation.domain.parcours.Jalon;
import cm.gimac.des.homologation.domain.parcours.Livrable;
import cm.gimac.des.homologation.domain.parcours.VueOperation;
import cm.gimac.des.homologation.domain.parcours.ModeleTypeProjet;
import cm.gimac.des.homologation.domain.parcours.NatureLivrable;
import cm.gimac.des.homologation.domain.parcours.Particularite;
import cm.gimac.des.homologation.domain.parcours.PlateformeExecution;
import cm.gimac.des.homologation.domain.parcours.Position;
import cm.gimac.des.homologation.domain.parcours.ReferentielOperation;
import cm.gimac.des.homologation.domain.parcours.SegmentParcours;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Conversion de l'annexe A du CR-CERT-GIMAC-01 en version du référentiel de chaque type de projet
 * (docs/lot1/modelisateur-processus.md §1 et §3). Deux éditions : v1.4 (fichier {@code parcours_referentiel_cr.csv},
 * autrefois lu en dur par le moteur, version 1 reprise par D-77, figée) et v1.7, qui fait foi (porteur Production
 * monétique des opérations M6, M11 à M14, M16 et M17, intitulés complets, jalon d'approbation de la sécurité sur le
 * rang 24 ; fichiers {@code *_v17.csv}, reprise {@code RepriseReferentielCrV17}). La conversion est fidèle au chemin historique : rangs 3 et suivants du tronc commun et branche du type ;
 * R le premier porteur, C les autres porteurs internes, E les externes, V le conducteur (le chef du DES pour le
 * conducteur), I le chef de la DDRM ; le rang 27 reçoit explicitement pour prédécesseur la dernière opération de
 * la branche (fin du raccord implicite) ; gestes, jalons, déclencheurs et particularités reprennent les rangs
 * autrefois codés en dur ; un livrable par opération de grain PHASE ou pivot, déposé par R. Le CSV ne sert plus
 * qu'aux reprises et aux jeux d'essai. Les libellés français viennent de {@code libelles_operations_cr*.csv}.
 */
public final class ConversionAnnexeA {

    /** Libellé de la version 1 reprise de l'annexe A du CR v1.4 (D-77) ; clé d'idempotence de sa reprise. */
    public static final String LIBELLE_VERSION = "Annexe A du CR-CERT-GIMAC-01 v1.4 (version 1 publiée, D-77)";
    /** Libellé de la version reprise de l'annexe A du CR v1.7 ; clé d'idempotence de sa reprise. */
    public static final String LIBELLE_VERSION_V17 = "Annexe A du CR-CERT-GIMAC-01 v1.7 (version publiée par reprise)";
    static final List<String> PDF = List.of("application/pdf");

    /**
     * Édition de l'annexe A : libellé de la version reprise, fichiers sources (jamais modifiés une fois repris : le
     * premier est chargé par un changeset appliqué) et rang qui porte le jalon d'approbation de la sécurité.
     */
    public enum Edition {
        /** CR v1.4 : version 1 reprise par D-77 ; le jalon était posé sur le dernier des rangs 23 à 25. */
        V1_4(LIBELLE_VERSION, "/config/liquibase/data/parcours_referentiel_cr.csv", "/config/liquibase/data/libelles_operations_cr.csv", "25"),
        /** CR v1.7, qui fait foi : l'approbation du changement est le rang 24 (A.4), qui déclenche aussi C1 (A.5). */
        V1_7(
            LIBELLE_VERSION_V17,
            "/config/liquibase/data/parcours_referentiel_cr_v17.csv",
            "/config/liquibase/data/libelles_operations_cr_v17.csv",
            "24"
        );

        private final String libelle;
        private final String csv;
        private final String libelles;
        private final String rangApprobation;

        Edition(String libelle, String csv, String libelles, String rangApprobation) {
            this.libelle = libelle;
            this.csv = csv;
            this.libelles = libelles;
            this.rangApprobation = rangApprobation;
        }

        public String libelle() {
            return libelle;
        }

        public String rangApprobation() {
            return rangApprobation;
        }
    }

    /** Édition qui fait foi (décision de l'utilisateur du 13/10/2026 : CR-CERT-GIMAC-01 v1.7). */
    public static final Edition EN_VIGUEUR = Edition.V1_7;

    /** Opération convertie : son code et la saisie qui la décrit. */
    public record Operation(String code, ReferenceOperationData data) {}

    private record Libelles(String libelle, String preuve, String valeur) {}

    private ConversionAnnexeA() {}

    /** Lignes du CSV de l'annexe A v1.4 (ancien chemin du moteur, conservé pour la reprise et la comparaison). */
    public static List<ReferentielOperation> lireCsv() {
        return lireCsv(Edition.V1_4);
    }

    /** Lignes du CSV d'une édition de l'annexe A. */
    public static List<ReferentielOperation> lireCsv(Edition edition) {
        List<ReferentielOperation> l = new ArrayList<>();
        for (String[] c : lignes(edition.csv)) {
            l.add(
                new ReferentielOperation(
                    c[0],
                    Integer.parseInt(c[1]),
                    EtapeCode.valueOf(c[2]),
                    SegmentParcours.valueOf(c[3]),
                    Arrays.stream(c[4].split("\\|")).map(Fonction::valueOf).toList(),
                    Arrays.stream(c[5].split("\\|")).map(TypeProjet::valueOf).toList(),
                    c[13].isBlank() ? List.of() : Arrays.asList(c[13].split("\\|"))
                ).attributs(
                    c[6].isBlank() ? null : PlateformeExecution.valueOf(c[6]),
                    GrainPreuve.valueOf(c[7]),
                    Boolean.parseBoolean(c[8]),
                    Boolean.parseBoolean(c[9]),
                    Boolean.parseBoolean(c[10]),
                    Boolean.parseBoolean(c[11]),
                    Boolean.parseBoolean(c[12])
                ).delai(Integer.parseInt(c[14].trim()))
            );
        }
        return l;
    }

    private static Map<String, Libelles> libelles(Edition edition) {
        Map<String, Libelles> m = new HashMap<>();
        for (String[] c : lignes(edition.libelles)) {
            m.put(c[0], new Libelles(c[1], c.length > 2 ? c[2] : "", c.length > 3 ? c[3] : ""));
        }
        return m;
    }

    private static List<String[]> lignes(String ressource) {
        try (InputStream in = ConversionAnnexeA.class.getResourceAsStream(ressource)) {
            if (in == null) {
                throw new IllegalStateException("Ressource absente : " + ressource);
            }
            String[] lignes = new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R");
            List<String[]> l = new ArrayList<>();
            for (int i = 1; i < lignes.length; i++) {
                if (!lignes[i].isBlank()) {
                    l.add(lignes[i].split(";", -1));
                }
            }
            return l;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Version 1 d'un type de projet (annexe A du CR v1.4, reprise D-77), dans l'ordre du référentiel. */
    public static List<Operation> version1(TypeProjet type) {
        return version(type, Edition.V1_4);
    }

    /** Version d'un type de projet fidèle à l'édition en vigueur de l'annexe A (CR v1.7). */
    public static List<Operation> enVigueur(TypeProjet type) {
        return version(type, EN_VIGUEUR);
    }

    /** Version d'un type de projet fidèle à une édition de l'annexe A, dans l'ordre du référentiel. */
    public static List<Operation> version(TypeProjet type, Edition edition) {
        Map<String, Libelles> libelles = libelles(edition);
        List<ReferentielOperation> suite = lireCsv(edition)
            .stream()
            .filter(o -> o.getTypes().contains(type))
            .filter(o -> !o.getEtape().horsParcours())
            .sorted(Comparator.comparingInt(ReferentielOperation::getOrdre))
            .toList();
        Set<String> codes = suite.stream().map(ReferentielOperation::getRang).collect(Collectors.toSet());
        String finBranche = suite.stream().filter(o -> o.getSegment() == SegmentParcours.BRANCHE).reduce((a, b) -> b).map(ReferentielOperation::getRang).orElse(null);
        Map<EtapeCode, Integer> lignesParEtape = new LinkedHashMap<>();
        List<Operation> ops = new ArrayList<>();
        for (ReferentielOperation o : suite) {
            String rang = o.getRang();
            List<String> preds = o.getSegment() == SegmentParcours.AVAL && o.getPredecesseurs().isEmpty() && finBranche != null
                ? List.of(finBranche)
                : o.getPredecesseurs().stream().filter(codes::contains).toList();
            List<Fonction> porteurs = o.getPorteurs();
            Fonction r = porteurs.get(0);
            List<Fonction> autres = porteurs.stream().skip(1).toList();
            Libelles lib = libelles.getOrDefault(rang, new Libelles(rang, "", ""));
            int ligne = lignesParEtape.merge(o.getEtape(), 1, Integer::sum) - 1;
            PlateformeExecution plateforme = o.getPlateforme();
            if ("15".equals(rang) || "16".equals(rang)) {
                plateforme = ModeleTypeProjet.parDefaut(type).plateformeEssais();
            }
            List<Livrable> livrables = o.getGrain() == GrainPreuve.PHASE || o.isPivot()
                ? List.of(new Livrable("L1", lib.preuve().isBlank() ? lib.libelle() : lib.preuve(), NatureLivrable.LIVRABLE, r, PDF, true))
                : List.of();
            ops.add(
                new Operation(
                    rang,
                    new ReferenceOperationData(
                        lib.libelle(),
                        null,
                        Segment.du(o.getSegment()),
                        null,
                        null,
                        null,
                        null,
                        null,
                        preds,
                        null,
                        null,
                        rang,
                        lib.preuve().isBlank() ? null : "Preuve attendue : " + lib.preuve() + ".",
                        o.getEtape().name(),
                        o.getOrdre(),
                        r,
                        r == Fonction.CONDUCTEUR ? Fonction.CHEF_DES : Fonction.CONDUCTEUR,
                        autres.stream().filter(f -> !f.externe()).toList(),
                        List.of(Fonction.CHEF_DDRM),
                        autres.stream().filter(Fonction::externe).toList(),
                        o.getDelaiJours(),
                        o.getGrain(),
                        plateforme,
                        o.isCle(),
                        o.isSecret(),
                        o.isOubliSansSymptome(),
                        o.isPivot(),
                        o.isValeurReference() ? (lib.valeur().isBlank() ? "Valeur de référence" : lib.valeur()) : null,
                        livrables,
                        geste(o),
                        jalon(rang, edition),
                        declencheur(rang),
                        new Position(o.getEtape().ordinal() * 300.0, ligne * 120.0 + o.getSegment().ordinal() * 40.0),
                        particularite(rang)
                    ).avecVue(VueOperation.duRang(rang))
                )
            );
        }
        return ops;
    }

    /** Geste des rangs autrefois codés en dur (Parcours, ParcoursProjection, EvenementsCertification, simulateur). */
    static Geste geste(ReferentielOperation o) {
        return switch (o.getRang()) {
            case "6" -> Geste.AFFECTER_RESSOURCES;
            case "12" -> Geste.COORDONNEES_TECHNIQUES;
            case "17" -> Geste.CONSTATER_COUVERTURE;
            case "18", "19", "20", "21", "22", "23", "24", "25" -> Geste.EVENEMENT_EXTERNE;
            case "32", "33" -> Geste.FICHE_SUIVI;
            case "7", "8", "9", "10", "11" -> Geste.DEPOSER_LIVRABLE;
            default -> o.getGrain() == GrainPreuve.PHASE ? Geste.DEPOSER_LIVRABLE : Geste.DEPOSER_PREUVE;
        };
    }

    /**
     * Jalon d'un rang. Les rangs 23 à 25 se closent ensemble par l'approbation de la sécurité ; le jalon, unique, est
     * sur le rang de l'approbation propre à l'édition : 24 en v1.7 (« Approbation du changement », A.4), 25 en v1.4.
     */
    static Jalon jalon(String rang, Edition edition) {
        if (rang.equals(edition.rangApprobation)) {
            return Jalon.APPROUVEE_SECURITE;
        }
        return switch (rang) {
            case "17" -> Jalon.APTE_CERTIFICATION;
            case "19" -> Jalon.CERTIFIEE;
            case "31" -> Jalon.EN_PRODUCTION;
            default -> null;
        };
    }

    static String declencheur(String rang) {
        return switch (rang) {
            case "18" -> Declencheurs.PV_ETABLI;
            case "19" -> Declencheurs.PV_SIGNE;
            case "20" -> Declencheurs.DOSSIER_CHANGEMENT;
            case "21", "22" -> Declencheurs.ATTESTATIONS_ARCHIVEES;
            case "23", "24", "25" -> Declencheurs.SECURITE_APPROUVEE;
            case "26" -> Declencheurs.FICHE_CHANGEMENT;
            default -> null;
        };
    }

    static Particularite particularite(String rang) {
        return switch (rang) {
            case "15" -> Particularite.ENTREE_ESSAIS;
            case "16" -> Particularite.FIN_ESSAIS;
            case "C1" -> Particularite.CEREMONIE_CLES;
            case "35" -> Particularite.SERVICE_SUPPLEMENTAIRE;
            default -> null;
        };
    }
}
