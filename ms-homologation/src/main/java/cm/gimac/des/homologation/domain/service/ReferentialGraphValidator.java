package cm.gimac.des.homologation.domain.service;

import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.parcours.CatalogueFonctions;
import cm.gimac.des.homologation.domain.parcours.Declencheurs;
import cm.gimac.des.homologation.domain.parcours.Geste;
import cm.gimac.des.homologation.domain.parcours.Jalon;
import cm.gimac.des.homologation.domain.parcours.SignatureOperation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Contrôle de cohérence du référentiel (HOM-02 A2, RG-26) : unicité des codes, existence des
 * prédécesseurs, compatibilité des segments, acyclicité. Toutes les anomalies sont recensées.
 * Règle de segments : le rang d'un prédécesseur (amont 0, branche 1, aval 2) ne dépasse jamais
 * celui de l'opération ; le tronc commun amont reste ainsi identique pour tous les types de projet.
 * Contrôles du modèle (D-77, docs/lot1/modelisateur-processus.md §2), sur les nœuds qui portent leur modèle :
 * responsable et validateur présents et distincts, au moins une opération sans prédécesseur, déclencheur connu
 * de chaque événement externe, jalons uniques et ordonnés le long du graphe, secret sans valeur de référence,
 * codes de livrables uniques. Les anomalies du modèle suivent celles du graphe. D-91 (docs/lot1/types-projet.md) :
 * fonctions citées actives au catalogue, signatures à codes uniques, signataire actif au catalogue et interne,
 * livrables portés existants (de l'opération, ou {@code OPERATION/LIVRABLE} d'une opération antérieure).
 */
public final class ReferentialGraphValidator {

    public enum TypeAnomalie {
        CODE_DUPLIQUE,
        PREDECESSEUR_INCONNU,
        SEGMENT_INCOHERENT,
        CYCLE,
        RESPONSABLE_ABSENT,
        VALIDATEUR_ABSENT,
        VALIDATEUR_EGAL_RESPONSABLE,
        DECLENCHEUR_INCONNU,
        JALON_DUPLIQUE,
        JALON_DESORDONNE,
        SANS_ENTREE,
        LIVRABLE_DUPLIQUE,
        SECRET_AVEC_VALEUR,
        FONCTION_INCONNUE,
        SIGNATURE_DUPLIQUEE,
        SIGNATAIRE_INCONNU,
        SIGNATAIRE_EXTERNE,
        SIGNATURE_SANS_LIVRABLE,
        SIGNATURE_PORTEE_INVALIDE,
        GESTE_CERTIFICATION_ABSENT,
    }

    /**
     * Gestes de certification attendus (D-93 §4, D-94, docs/lot1/effet-reel-referentiels.md §10.5) : pour un jalon porté
     * par la version, les déclencheurs (événements de MS-CERTIFICATION) qu'au moins une opération doit porter, sans quoi
     * le geste correspondant serait refusé à l'exécution (CER_OPERATION_ABSENTE). {@code avecAttestations} s'ajoute quand
     * le modèle du type de projet exige des attestations. Données de configuration, aucune liste dans le code.
     */
    public record ExigencesCertification(Map<Jalon, List<String>> toujours, Map<Jalon, List<String>> avecAttestations) {
        public ExigencesCertification {
            toujours = toujours == null ? Map.of() : Map.copyOf(toujours);
            avecAttestations = avecAttestations == null ? Map.of() : Map.copyOf(avecAttestations);
        }

        public static ExigencesCertification aucune() {
            return new ExigencesCertification(Map.of(), Map.of());
        }

        List<String> pour(Jalon j, boolean attestations) {
            List<String> l = new ArrayList<>(toujours.getOrDefault(j, List.of()));
            if (attestations) {
                avecAttestations.getOrDefault(j, List.of()).stream().filter(x -> !l.contains(x)).forEach(l::add);
            }
            return l;
        }
    }

    public record Anomalie(TypeAnomalie type, List<String> codes, String motif) {}

    public record Rapport(List<Anomalie> anomalies, List<String> ordre, Map<String, Integer> couches) {
        @com.fasterxml.jackson.annotation.JsonProperty("conforme")
        public boolean conforme() {
            return anomalies.isEmpty();
        }
    }

    private final TopologicalSorter sorter;
    private final ExigencesCertification exigences;

    public ReferentialGraphValidator(TopologicalSorter sorter) {
        this(sorter, ExigencesCertification.aucune());
    }

    public ReferentialGraphValidator(TopologicalSorter sorter, ExigencesCertification exigences) {
        this.sorter = sorter;
        this.exigences = exigences == null ? ExigencesCertification.aucune() : exigences;
    }

    public ReferentialGraphValidator() {
        this(new TopologicalSorter());
    }

    public Rapport controler(List<NoeudReferentiel> noeuds) {
        return controler(noeuds, true);
    }

    /** {@code attestations} : le modèle du type de projet exige des attestations (gestes d'attestation attendus). */
    public Rapport controler(List<NoeudReferentiel> noeuds, boolean attestations) {
        List<Anomalie> anomalies = new ArrayList<>();
        Map<String, NoeudReferentiel> parCode = new TreeMap<>();
        for (NoeudReferentiel n : noeuds) {
            if (parCode.putIfAbsent(n.code(), n) != null) {
                anomalies.add(new Anomalie(TypeAnomalie.CODE_DUPLIQUE, List.of(n.code()), "Code présent plusieurs fois"));
            }
        }
        Map<String, List<String>> arcsValides = new TreeMap<>();
        for (NoeudReferentiel n : parCode.values()) {
            List<String> valides = new ArrayList<>();
            for (String p : n.predecesseurs()) {
                NoeudReferentiel pred = parCode.get(p);
                if (pred == null) {
                    anomalies.add(new Anomalie(TypeAnomalie.PREDECESSEUR_INCONNU, List.of(n.code(), p), "Prédécesseur absent de la version"));
                    continue;
                }
                if (pred.segment() != null && n.segment() != null && pred.segment().ordinal() > n.segment().ordinal()) {
                    anomalies.add(
                        new Anomalie(
                            TypeAnomalie.SEGMENT_INCOHERENT,
                            List.of(n.code(), p),
                            "Une opération du segment " + n.segment() + " ne peut succéder à une opération du segment " + pred.segment()
                        )
                    );
                }
                valides.add(p);
            }
            arcsValides.put(n.code(), valides);
        }
        TopologicalSorter.Result tri = sorter.sort(arcsValides);
        if (!tri.acyclique()) {
            anomalies.add(new Anomalie(TypeAnomalie.CYCLE, tri.cycle(), "Cycle dans le graphe des prédécesseurs"));
        }
        if (parCode.values().stream().anyMatch(n -> n.modele() != null)) {
            controlerModele(parCode, arcsValides, tri.acyclique(), anomalies);
            controlerGestesCertification(parCode, attestations, anomalies);
        }
        return new Rapport(List.copyOf(anomalies), tri.ordre(), tri.couches());
    }

    /** GESTE_CERTIFICATION_ABSENT : un jalon porté sans les déclencheurs des gestes de certification qu'il exige. */
    private void controlerGestesCertification(Map<String, NoeudReferentiel> parCode, boolean attestations, List<Anomalie> anomalies) {
        Set<Jalon> jalons = new LinkedHashSet<>();
        Set<String> declencheurs = new HashSet<>();
        for (NoeudReferentiel n : parCode.values()) {
            if (n.modele() == null) {
                continue;
            }
            if (n.modele().jalon() != null) {
                jalons.add(n.modele().jalon());
            }
            if (n.modele().declencheur() != null) {
                declencheurs.add(n.modele().declencheur());
            }
        }
        for (Jalon j : Jalon.values()) {
            if (!jalons.contains(j)) {
                continue;
            }
            List<String> manquants = exigences.pour(j, attestations).stream().filter(d -> !declencheurs.contains(d)).toList();
            if (!manquants.isEmpty()) {
                List<String> codes = new ArrayList<>();
                codes.add(j.name());
                codes.addAll(manquants);
                anomalies.add(
                    new Anomalie(
                        TypeAnomalie.GESTE_CERTIFICATION_ABSENT,
                        List.copyOf(codes),
                        // Recette HOM-02 : motif en libellés (jalon, gestes), les codes restent dans {@code codes}.
                        "Le jalon « " +
                        j.libelle() +
                        " » exige une opération qui porte chaque geste de certification : " +
                        String.join(", ", manquants.stream().map(d -> Declencheurs.CONNUS.getOrDefault(d, d)).toList()) +
                        "."
                    )
                );
            }
        }
    }

    private static void controlerModele(Map<String, NoeudReferentiel> parCode, Map<String, List<String>> arcs, boolean acyclique, List<Anomalie> anomalies) {
        Map<Jalon, List<String>> jalons = new EnumMap<>(Jalon.class);
        for (NoeudReferentiel n : parCode.values()) {
            NoeudReferentiel.Modele m = n.modele();
            if (m == null) {
                continue;
            }
            if (m.responsable() == null) {
                anomalies.add(new Anomalie(TypeAnomalie.RESPONSABLE_ABSENT, List.of(n.code()), "Aucun responsable (R) désigné"));
            }
            if (m.validateur() == null) {
                anomalies.add(new Anomalie(TypeAnomalie.VALIDATEUR_ABSENT, List.of(n.code()), "Aucun validateur (V) désigné"));
            } else if (m.validateur().equals(m.responsable())) {
                anomalies.add(new Anomalie(TypeAnomalie.VALIDATEUR_EGAL_RESPONSABLE, List.of(n.code()), "Le validateur doit être distinct du responsable"));
            }
            if ((m.geste() == Geste.EVENEMENT_EXTERNE && m.declencheur() == null) || (m.declencheur() != null && !Declencheurs.connu(m.declencheur()))) {
                anomalies.add(
                    new Anomalie(
                        TypeAnomalie.DECLENCHEUR_INCONNU,
                        m.declencheur() == null ? List.of(n.code()) : List.of(n.code(), m.declencheur()),
                        "Un événement externe doit désigner un sujet connu"
                    )
                );
            }
            if (m.secret() && m.valeurReference() != null) {
                anomalies.add(new Anomalie(TypeAnomalie.SECRET_AVEC_VALEUR, List.of(n.code()), "Une opération secrète ne relève aucune valeur (D-59)"));
            }
            Set<String> vus = new HashSet<>();
            Set<String> doublons = new LinkedHashSet<>();
            m.livrables().forEach(c -> {
                if (!vus.add(c)) {
                    doublons.add(c);
                }
            });
            if (!doublons.isEmpty()) {
                List<String> codes = new ArrayList<>();
                codes.add(n.code());
                codes.addAll(doublons);
                anomalies.add(new Anomalie(TypeAnomalie.LIVRABLE_DUPLIQUE, List.copyOf(codes), "Code de livrable présent plusieurs fois"));
            }
            if (m.jalon() != null) {
                jalons.computeIfAbsent(m.jalon(), j -> new ArrayList<>()).add(n.code());
            }
            m
                .fonctions()
                .stream()
                .filter(f -> !CatalogueFonctions.active(f))
                .forEach(f -> anomalies.add(new Anomalie(TypeAnomalie.FONCTION_INCONNUE, List.of(n.code(), f.name()), "Fonction absente du catalogue ou désactivée")));
            controlerSignatures(n, parCode, arcs, acyclique, anomalies);
        }
        if (!parCode.isEmpty() && parCode.values().stream().noneMatch(n -> n.predecesseurs().isEmpty())) {
            anomalies.add(new Anomalie(TypeAnomalie.SANS_ENTREE, List.of(), "Aucune opération sans prédécesseur : le parcours n'a pas d'entrée"));
        }
        jalons.forEach((j, codes) -> {
            if (codes.size() > 1) {
                List<String> l = new ArrayList<>();
                l.add(j.name());
                l.addAll(codes);
                anomalies.add(new Anomalie(TypeAnomalie.JALON_DUPLIQUE, List.copyOf(l), "Le jalon " + j.name() + " est porté par plusieurs opérations"));
            }
        });
        if (acyclique) {
            for (var tardif : jalons.entrySet()) {
                for (var precoce : jalons.entrySet()) {
                    if (precoce.getKey().ordinal() >= tardif.getKey().ordinal()) {
                        continue;
                    }
                    for (String t : tardif.getValue()) {
                        for (String p : precoce.getValue()) {
                            if (ancetre(t, p, arcs)) {
                                anomalies.add(
                                    new Anomalie(
                                        TypeAnomalie.JALON_DESORDONNE,
                                        List.of(t, p),
                                        "Le jalon " + tardif.getKey().name() + " précède le jalon antérieur " + precoce.getKey().name()
                                    )
                                );
                            }
                        }
                    }
                }
            }
        }
    }

    private static void controlerSignatures(
        NoeudReferentiel n,
        Map<String, NoeudReferentiel> parCode,
        Map<String, List<String>> arcs,
        boolean acyclique,
        List<Anomalie> anomalies
    ) {
        Set<String> codes = new HashSet<>();
        for (SignatureOperation s : n.modele().signatures()) {
            if (!codes.add(s.code())) {
                anomalies.add(new Anomalie(TypeAnomalie.SIGNATURE_DUPLIQUEE, List.of(n.code(), s.code()), "Code de signature présent plusieurs fois"));
            }
            if (s.signataire() == null || !CatalogueFonctions.active(s.signataire())) {
                anomalies.add(
                    new Anomalie(
                        TypeAnomalie.SIGNATAIRE_INCONNU,
                        s.signataire() == null ? List.of(n.code(), s.code()) : List.of(n.code(), s.code(), s.signataire().name()),
                        "Le signataire doit être une fonction active du catalogue"
                    )
                );
            } else if (s.signataire().externe()) {
                anomalies.add(
                    new Anomalie(TypeAnomalie.SIGNATAIRE_EXTERNE, List.of(n.code(), s.code(), s.signataire().name()), "Une signature d'opération est donnée par un membre du GIMAC")
                );
            }
            if (s.porte().isEmpty()) {
                anomalies.add(new Anomalie(TypeAnomalie.SIGNATURE_SANS_LIVRABLE, List.of(n.code(), s.code()), "Une signature porte au moins un livrable"));
            }
            for (String ref : s.porte()) {
                String op = SignatureOperation.operationDe(ref);
                String livrable = SignatureOperation.livrableDe(ref);
                NoeudReferentiel porteur = op == null ? n : parCode.get(op);
                boolean valide =
                    porteur != null &&
                    porteur.modele() != null &&
                    porteur.modele().livrables().contains(livrable) &&
                    (op == null || (acyclique && ancetre(op, n.code(), arcs)));
                if (!valide) {
                    anomalies.add(
                        new Anomalie(
                            TypeAnomalie.SIGNATURE_PORTEE_INVALIDE,
                            List.of(n.code(), s.code(), ref),
                            "Livrable porté inconnu, ou d'une opération qui n'est pas antérieure"
                        )
                    );
                }
            }
        }
    }

    /** {@code a} est-il un ancêtre (prédécesseur transitif) de {@code b} ? */
    private static boolean ancetre(String a, String b, Map<String, List<String>> arcs) {
        Deque<String> pile = new ArrayDeque<>(arcs.getOrDefault(b, List.of()));
        Set<String> vus = new HashSet<>();
        while (!pile.isEmpty()) {
            String x = pile.pop();
            if (x.equals(a)) {
                return true;
            }
            if (vus.add(x)) {
                pile.addAll(arcs.getOrDefault(x, List.of()));
            }
        }
        return false;
    }

    /** Port interne invoqué par la publication (HOM-01) : renvoie l'ordre de référence ou refuse. */
    public List<String> validate(List<NoeudReferentiel> noeuds) {
        return validate(noeuds, true);
    }

    public List<String> validate(List<NoeudReferentiel> noeuds, boolean attestations) {
        Rapport r = controler(noeuds, attestations);
        if (!r.conforme()) {
            throw HomologationException.conflict(
                "HOM_REFERENTIAL_GRAPH_INVALID",
                "Publication refusée : graphe des prédécesseurs non conforme",
                Map.of("anomalies", r.anomalies())
            );
        }
        return r.ordre();
    }
}
