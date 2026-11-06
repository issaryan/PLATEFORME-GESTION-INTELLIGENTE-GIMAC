package cm.gimac.des.pilotage.domain.processus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.pilotage.domain.exception.PilotageException;
import cm.gimac.des.pilotage.domain.service.ControleProcessus;
import cm.gimac.des.pilotage.infrastructure.demo.DemonstrationProcessus;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CodesEtapesTest {

    static final String CODE = "POLE_RESEAU-PRC-7";

    @Test
    void clesProvisoiresReecritesGrapheInchange() {
        List<Etape> saisies = DemonstrationProcessus.etapes();
        List<Etape> l = CodesEtapes.reecrire(CODE, saisies, Set.of());
        assertThat(l).extracting(Etape::code).containsExactly(java.util.stream.IntStream.rangeClosed(1, saisies.size()).mapToObj(i -> CODE + "-E" + i).toArray(String[]::new));
        // Même graphe : chaque arête d'origine relie les mêmes étapes, désignées par leur nouveau code.
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < saisies.size(); i++) {
            m.put(saisies.get(i).code(), l.get(i).code());
        }
        for (int i = 0; i < saisies.size(); i++) {
            assertThat(l.get(i).predecesseurs()).containsExactlyElementsOf(saisies.get(i).predecesseurs().stream().map(m::get).toList());
            assertThat(l.get(i).libelle()).isEqualTo(saisies.get(i).libelle());
            assertThat(l.get(i).signatures()).isEqualTo(saisies.get(i).signatures());
        }
        // Graphe valide avant, valide après (aucune anomalie de structure : arêtes, cycles, étapes isolées).
        assertThat(ControleProcessus.structure(saisies)).isEmpty();
        assertThat(ControleProcessus.structure(l)).isEmpty();
    }

    @Test
    void codesConnusGardesNouvellesClesNumeroteesApres() {
        Etape a = etape(CODE + "-E2", List.of());
        Etape ancienne = etape("ANALYSE", List.of(CODE + "-E2"));
        Etape nouvelle = etape("NOUVELLE", List.of("ANALYSE", "INCONNUE"));
        List<Etape> l = CodesEtapes.reecrire(CODE, List.of(a, ancienne, nouvelle), Set.of(CODE + "-E2", "ANALYSE", CODE + "-E5"));
        assertThat(l).extracting(Etape::code).containsExactly(CODE + "-E2", "ANALYSE", CODE + "-E6");
        // Prédécesseur connu gardé ; référence inconnue laissée au contrôle (anomalie), jamais inventée.
        assertThat(l.get(2).predecesseurs()).containsExactly("ANALYSE", "INCONNUE");
    }

    @Test
    void cleEnDoubleRefuseeEtCodeBorneAQuaranteCaracteres() {
        assertThatThrownBy(() -> CodesEtapes.reecrire(CODE, List.of(etape("X", List.of()), etape("X", List.of())), Set.of()))
            .isInstanceOf(PilotageException.class).hasMessageContaining("double");
        String long40 = "UNITE_AU_NOM_TRES_LONG_DE_32_CAR-PRC-12";
        assertThat(CodesEtapes.code(long40, 3)).hasSize(40).endsWith("-E3");
        assertThat(CodesEtapes.code(CODE, 3)).isEqualTo(CODE + "-E3");
    }

    static Etape etape(String code, List<String> preds) {
        return new Etape(code, code, "", new Intervenant(ModeIntervenant.LANCEUR, null, null, null, null), null, preds, 1, List.of(), List.of(), List.of(), null);
    }
}
