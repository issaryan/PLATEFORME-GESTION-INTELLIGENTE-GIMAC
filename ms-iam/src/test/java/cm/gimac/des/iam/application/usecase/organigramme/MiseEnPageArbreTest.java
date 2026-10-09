package cm.gimac.des.iam.application.usecase.organigramme;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Mise en page de l'organigramme descendant (D-102 §4) : cartes sans chevauchement, parents centrés, feuilles empilées. */
class MiseEnPageArbreTest {

    static final double L = MiseEnPageArbre.LARGEUR;
    static final double H = MiseEnPageArbre.HAUTEUR;

    static MiseEnPageArbre.Plan plan(List<String> racines, Map<String, List<String>> arbre) {
        return MiseEnPageArbre.planifier(racines, k -> arbre.getOrDefault(k, List.of()));
    }

    static void sansChevauchement(MiseEnPageArbre.Plan p) {
        List<MiseEnPageArbre.Position> l = new ArrayList<>(p.positions().values());
        for (int i = 0; i < l.size(); i++) {
            for (int j = i + 1; j < l.size(); j++) {
                MiseEnPageArbre.Position a = l.get(i);
                MiseEnPageArbre.Position b = l.get(j);
                boolean disjoint = a.x() + L <= b.x() || b.x() + L <= a.x() || a.y() + H <= b.y() || b.y() + H <= a.y();
                assertThat(disjoint).as("cartes %s et %s", a, b).isTrue();
            }
            assertThat(l.get(i).x()).isGreaterThanOrEqualTo(0);
            assertThat(l.get(i).x() + L).isLessThanOrEqualTo(p.largeur() + 0.1);
            assertThat(l.get(i).y() + H).isLessThanOrEqualTo(p.hauteur() + 0.1);
        }
    }

    @Test
    @DisplayName("Organisation du GIMAC : toutes les unités placées, sans chevauchement, parent centré sur ses sous-unités")
    void organisation() {
        Map<String, List<String>> arbre = Map.of(
            "DG", List.of("CONFORMITE", "DDRM", "DES", "RH", "SSI"),
            "DES", List.of("PROD_INFO", "PROD_MONETIQUE", "SNA"),
            "PROD_INFO", List.of("POLE_RESEAU", "POLE_SYSTEME")
        );
        MiseEnPageArbre.Plan p = plan(List.of("DG"), arbre);
        assertThat(p.positions()).hasSize(11);
        sansChevauchement(p);
        // Sous-unités côte à côte sur une même ligne, sous leur parent.
        double yDg = p.positions().get("DG").y();
        assertThat(p.positions().get("DES").y()).isEqualTo(p.positions().get("SSI").y()).isGreaterThan(yDg + H);
        double premier = p.positions().get("CONFORMITE").x() + L / 2;
        double dernier = p.positions().get("SSI").x() + L / 2;
        assertThat(p.positions().get("DG").x() + L / 2).isCloseTo((premier + dernier) / 2, org.assertj.core.data.Offset.offset(0.2));
        // Deux pôles feuilles : empilés sous le service, en retrait, l'un sous l'autre.
        MiseEnPageArbre.Position info = p.positions().get("PROD_INFO");
        MiseEnPageArbre.Position reseau = p.positions().get("POLE_RESEAU");
        MiseEnPageArbre.Position systeme = p.positions().get("POLE_SYSTEME");
        assertThat(reseau.x()).isEqualTo(systeme.x()).isGreaterThan(info.x());
        assertThat(systeme.y()).isGreaterThan(reseau.y() + H);
        assertThat(p.traits()).isNotEmpty().allMatch(d -> d.startsWith("M"));
    }

    @Test
    @DisplayName("Forêt, unité seule, cycle hérité : chaque code placé une seule fois")
    void foretEtCycle() {
        MiseEnPageArbre.Plan p = plan(List.of("A", "B", "A"), Map.of("A", List.of("A1", "B"), "A1", List.of("A")));
        assertThat(p.positions().keySet()).containsExactly("A", "A1", "B");
        sansChevauchement(p);
        MiseEnPageArbre.Plan seule = plan(List.of("X"), Map.of());
        assertThat(seule.largeur()).isEqualTo(L);
        assertThat(seule.hauteur()).isEqualTo(H);
        assertThat(seule.traits()).isEmpty();
        assertThat(plan(List.of(), Map.of()).largeur()).isZero();
    }

    @Test
    @DisplayName("Une seule sous-unité : alignée sous son parent ; branches larges : pas de chevauchement")
    void branches() {
        MiseEnPageArbre.Plan p = plan(List.of("R"), Map.of("R", List.of("A"), "A", List.of("B", "C"), "B", List.of("B1", "B2", "B3"), "C", List.of("C1")));
        sansChevauchement(p);
        assertThat(p.positions().get("R").x()).isEqualTo(p.positions().get("A").x());
    }
}
