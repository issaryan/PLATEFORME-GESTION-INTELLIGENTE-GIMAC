package cm.gimac.des.homologation.domain.service;

import cm.gimac.des.homologation.domain.enumeration.Segment;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Graphes de test reproductibles (graine fixe) pour HOM-02 et HOM-07. */
public final class GraphSamples {

    private GraphSamples() {}

    /**
     * Graphe orienté acyclique aléatoire : les nœuds sont numérotés, un arc ne va que d'un nœud
     * de rang inférieur vers un nœud de rang supérieur ; le segment croît avec le rang.
     */
    public static List<NoeudReferentiel> dagAleatoire(int taille, double densite, long graine) {
        Random aleatoire = new Random(graine);
        List<NoeudReferentiel> noeuds = new ArrayList<>(taille);
        for (int i = 0; i < taille; i++) {
            List<String> preds = new ArrayList<>();
            for (int j = 0; j < i; j++) {
                if (aleatoire.nextDouble() < densite) {
                    preds.add(code(j));
                }
            }
            Segment segment = i < taille / 3 ? Segment.TRONC_COMMUN_AMONT : i < 2 * taille / 3 ? Segment.BRANCHE : Segment.TRONC_COMMUN_AVAL;
            // Un nœud amont ne dépend que de l'amont : les arcs retenus respectent déjà l'ordre des segments.
            noeuds.add(new NoeudReferentiel(code(i), code(i), segment, preds));
        }
        return noeuds;
    }

    private static String code(int i) {
        return "OP-%04d".formatted(i);
    }
}
