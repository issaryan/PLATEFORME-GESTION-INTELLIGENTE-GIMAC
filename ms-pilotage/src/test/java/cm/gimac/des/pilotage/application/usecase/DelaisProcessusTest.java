package cm.gimac.des.pilotage.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.pilotage.application.port.in.ProcessusUseCase.EtapeSaisie;
import cm.gimac.des.pilotage.application.port.in.ProcessusUseCase.IntervenantSaisie;
import cm.gimac.des.pilotage.domain.exception.PilotageException;
import cm.gimac.des.pilotage.domain.processus.Etape;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Délai cible et délai maximal (D-106) : saisie du processus et de ses étapes, contrôle maximum >= cible. */
class DelaisProcessusTest {

    private static void refus(Runnable r, String champ) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(PilotageException.class, e -> {
            assertThat(e.code()).isEqualTo("PIL_REQUETE_INVALIDE");
            assertThat(e.details()).containsEntry("champ", champ);
        });
    }

    private static EtapeSaisie etape(Integer cible, Integer max) {
        return new EtapeSaisie("E", "Étape", null, new IntervenantSaisie("LANCEUR", null, null, null, null), null, List.of(), cible, List.of(), List.of(), List.of(), null, max);
    }

    @Test
    @DisplayName("Processus : maximum facultatif, borné, au moins le délai cible")
    void processus() {
        assertThat(SaisieProcessus.delaiMax("delaiMaxJours", null, 5)).isNull();
        assertThat(SaisieProcessus.delaiMax("delaiMaxJours", 5, 5)).isEqualTo(5);
        assertThat(SaisieProcessus.delaiMax("delaiMaxJours", 9, null)).isEqualTo(9);
        refus(() -> SaisieProcessus.delaiMax("delaiMaxJours", 4, 5), "delaiMaxJours");
        refus(() -> SaisieProcessus.delaiMax("delaiMaxJours", 366, null), "delaiMaxJours");
        refus(() -> SaisieProcessus.delaiMax("delaiMaxJours", -1, null), "delaiMaxJours");
    }

    @Test
    @DisplayName("Étapes : délai maximal repris dans la définition, refusé en deçà du délai cible")
    void etapes() {
        Etape e = SaisieProcessus.etapes(List.of(etape(3, 7))).get(0);
        assertThat(e.delaiJours()).isEqualTo(3);
        assertThat(e.delaiMaxJours()).isEqualTo(7);
        assertThat(e.delaiMaxEffectif()).isEqualTo(7);
        Etape sans = SaisieProcessus.etapes(List.of(etape(3, null))).get(0);
        assertThat(sans.delaiMaxJours()).isNull();
        assertThat(sans.delaiMaxEffectif()).isEqualTo(3);
        refus(() -> SaisieProcessus.etapes(List.of(etape(3, 2))), "etapes[0].delaiMaxJours");
    }
}
