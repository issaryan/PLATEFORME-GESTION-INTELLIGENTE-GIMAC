package cm.gimac.des.iam.application.usecase.organigramme;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Mise en page d'un organigramme descendant (D-102, docs/lot1/organigramme.md §4), sans dépendance : chaque unité
 * reçoit une carte de taille fixe ; les sous-unités d'une unité sont posées côte à côte sous elle, centrées, et reliées
 * par des traits coudés ; quand toutes les sous-unités d'une unité sont des feuilles (au moins deux), elles sont
 * empilées en colonne sous elle, reliées par une épine verticale, pour garder un document lisible en largeur. Plusieurs
 * racines (plusieurs directions) sont posées côte à côte. Coordonnées en pixels CSS, origine en haut à gauche.
 */
public final class MiseEnPageArbre {

    /** Largeur et hauteur d'une carte. */
    public static final double LARGEUR = 236;
    public static final double HAUTEUR = 112;
    static final double ECART_X = 28;
    static final double ECART_Y = 60;
    static final double PILE_RETRAIT = 40;
    static final double PILE_ECART = 14;
    static final int PROFONDEUR_MAX = 32;

    /** Coin haut gauche d'une carte. */
    public record Position(double x, double y) {}

    /**
     * Résultat : position de chaque carte (dans l'ordre du parcours), tracés SVG des liaisons (attribut {@code d}),
     * dimensions totales.
     */
    public record Plan(Map<String, Position> positions, List<String> traits, double largeur, double hauteur) {}

    private record Mesure(double largeur, double hauteur, double centre) {}

    private final Function<String, List<String>> enfants;
    private final Map<String, Mesure> mesures = new LinkedHashMap<>();
    private final Map<String, Position> positions = new LinkedHashMap<>();
    private final List<String> traits = new ArrayList<>();
    private final Set<String> vus = new HashSet<>();

    private MiseEnPageArbre(Function<String, List<String>> enfants) {
        this.enfants = enfants;
    }

    /**
     * Plan d'une forêt.
     *
     * @param racines codes des racines, dans l'ordre d'affichage
     * @param enfants sous-unités d'une unité, dans l'ordre d'affichage (un code déjà placé est ignoré : pas de cycle)
     */
    public static Plan planifier(List<String> racines, Function<String, List<String>> enfants) {
        MiseEnPageArbre m = new MiseEnPageArbre(enfants);
        List<String> r = racines.stream().filter(m.vus::add).toList();
        double x = 0;
        double hauteur = 0;
        for (String c : r) {
            Mesure me = m.mesurer(c, 0);
            m.placer(c, x, 0, 0);
            x += me.largeur() + ECART_X * 2;
            hauteur = Math.max(hauteur, me.hauteur());
        }
        double largeur = r.isEmpty() ? 0 : x - ECART_X * 2;
        return new Plan(java.util.Collections.unmodifiableMap(new LinkedHashMap<>(m.positions)), List.copyOf(m.traits), largeur, hauteur);
    }

    private List<String> sous(String code, int profondeur) {
        if (profondeur >= PROFONDEUR_MAX) {
            return List.of();
        }
        List<String> l = new ArrayList<>();
        for (String e : enfants.apply(code)) {
            if (!mesures.containsKey(e) && vus.add(e)) {
                l.add(e);
            }
        }
        return l;
    }

    private final Map<String, List<String>> retenus = new LinkedHashMap<>();
    private final Set<String> empiles = new HashSet<>();

    private Mesure mesurer(String code, int profondeur) {
        List<String> l = sous(code, profondeur);
        retenus.put(code, l);
        Mesure m;
        if (l.isEmpty()) {
            m = new Mesure(LARGEUR, HAUTEUR, LARGEUR / 2);
        } else {
            List<Mesure> ms = new ArrayList<>();
            for (String e : l) {
                ms.add(mesurer(e, profondeur + 1));
            }
            boolean feuilles = l.size() >= 2 && l.stream().allMatch(e -> retenus.get(e).isEmpty());
            if (feuilles) {
                empiles.add(code);
                m = new Mesure(LARGEUR + PILE_RETRAIT, HAUTEUR + l.size() * (HAUTEUR + PILE_ECART), LARGEUR / 2);
            } else {
                double total = 0;
                double premier = 0;
                double dernier = 0;
                double haut = 0;
                for (int i = 0; i < ms.size(); i++) {
                    Mesure x = ms.get(i);
                    if (i == 0) {
                        premier = total + x.centre();
                    }
                    dernier = total + x.centre();
                    total += x.largeur() + (i < ms.size() - 1 ? ECART_X : 0);
                    haut = Math.max(haut, x.hauteur());
                }
                double centre = (premier + dernier) / 2;
                double decalage = Math.max(0, LARGEUR / 2 - centre);
                m = new Mesure(Math.max(total + decalage, centre + decalage + LARGEUR / 2), HAUTEUR + ECART_Y + haut, centre + decalage);
            }
        }
        mesures.put(code, m);
        return m;
    }

    /** Place la carte de {@code code} dans la boîte de sous-arbre d'abscisse {@code gauche}. */
    private void placer(String code, double gauche, double haut, int profondeur) {
        Mesure m = mesures.get(code);
        double x = gauche + m.centre() - LARGEUR / 2;
        positions.put(code, new Position(arrondi(x), arrondi(haut)));
        List<String> l = retenus.getOrDefault(code, List.of());
        if (l.isEmpty()) {
            return;
        }
        if (empiles.contains(code)) {
            double epine = x + PILE_RETRAIT / 2;
            double y = haut + HAUTEUR + PILE_ECART;
            double dernierMilieu = y;
            for (String e : l) {
                positions.put(e, new Position(arrondi(x + PILE_RETRAIT), arrondi(y)));
                dernierMilieu = y + HAUTEUR / 2;
                traits.add("M" + f(epine) + " " + f(dernierMilieu) + "H" + f(x + PILE_RETRAIT));
                y += HAUTEUR + PILE_ECART;
            }
            traits.add("M" + f(epine) + " " + f(haut + HAUTEUR) + "V" + f(dernierMilieu));
            return;
        }
        double milieuX = x + LARGEUR / 2;
        double niveau = haut + HAUTEUR + ECART_Y / 2;
        double haute = haut + HAUTEUR + ECART_Y;
        double total = 0;
        for (String e : l) {
            total += mesures.get(e).largeur();
        }
        total += ECART_X * (l.size() - 1);
        // Les sous-arbres sont centrés sous la carte quand elle est plus large qu'eux.
        double premierCentre = mesures.get(l.get(0)).centre();
        double dernierCentre = total - mesures.get(l.get(l.size() - 1)).largeur() + mesures.get(l.get(l.size() - 1)).centre();
        double depart = milieuX - (premierCentre + dernierCentre) / 2;
        double cx = depart;
        double gaucheTrait = Double.MAX_VALUE;
        double droiteTrait = -Double.MAX_VALUE;
        for (String e : l) {
            Mesure me = mesures.get(e);
            placer(e, cx, haute, profondeur + 1);
            double centre = cx + me.centre();
            gaucheTrait = Math.min(gaucheTrait, centre);
            droiteTrait = Math.max(droiteTrait, centre);
            traits.add("M" + f(centre) + " " + f(niveau) + "V" + f(haute));
            cx += me.largeur() + ECART_X;
        }
        traits.add("M" + f(milieuX) + " " + f(haut + HAUTEUR) + "V" + f(niveau));
        if (droiteTrait > gaucheTrait) {
            traits.add("M" + f(gaucheTrait) + " " + f(niveau) + "H" + f(droiteTrait));
        }
    }

    private static double arrondi(double v) {
        return Math.round(v * 10) / 10.0;
    }

    private static String f(double v) {
        double a = arrondi(v);
        return a == Math.rint(a) ? Long.toString((long) a) : Double.toString(a);
    }
}
