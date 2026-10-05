package cm.gimac.des.pilotage.domain.activite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.pilotage.domain.exception.PilotageException;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PeriodeTest {

    @Test
    @DisplayName("Semaine ISO : du lundi au dimanche, y compris à cheval sur deux années")
    void semainesIso() {
        Periode s41 = Periode.de(TypeRapport.HEBDOMADAIRE, "2026-W41");
        assertThat(s41.debut()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(s41.fin()).isEqualTo(LocalDate.of(2026, 10, 11));
        assertThat(s41.semaine()).isEqualTo(41);
        Periode s1 = Periode.de(TypeRapport.HEBDOMADAIRE, "2026-W01");
        assertThat(s1.debut()).isEqualTo(LocalDate.of(2025, 12, 29));
        assertThat(Periode.de(TypeRapport.HEBDOMADAIRE, "2026-W53").debut()).isEqualTo(LocalDate.of(2026, 12, 28));
        assertThat(Periode.contenant(TypeRapport.HEBDOMADAIRE, LocalDate.of(2026, 1, 1)).texte()).isEqualTo("2026-W01");
        assertThat(Periode.contenant(TypeRapport.HEBDOMADAIRE, LocalDate.of(2026, 10, 11)).texte()).isEqualTo("2026-W41");
    }

    @Test
    @DisplayName("Jour : AAAA-MM-JJ ; période future si elle commence après aujourd'hui (semaine en cours admise)")
    void jourEtFutur() {
        Periode j = Periode.de(TypeRapport.QUOTIDIEN, "2026-10-06");
        assertThat(j.debut()).isEqualTo(j.fin());
        LocalDate mercredi = LocalDate.of(2026, 10, 7);
        assertThat(j.estFuture(mercredi)).isFalse();
        assertThat(Periode.de(TypeRapport.QUOTIDIEN, "2026-10-08").estFuture(mercredi)).isTrue();
        assertThat(Periode.de(TypeRapport.HEBDOMADAIRE, "2026-W41").estFuture(mercredi)).isFalse();
        assertThat(Periode.de(TypeRapport.HEBDOMADAIRE, "2026-W42").estFuture(mercredi)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "2026-13-01", "2026-02-30", "06/10/2026", "2026-W41", "" })
    @DisplayName("Période quotidienne mal formée : 400 PIL_PERIODE_INVALIDE")
    void jourInvalide(String texte) {
        assertThatThrownBy(() -> Periode.de(TypeRapport.QUOTIDIEN, texte)).isInstanceOfSatisfying(PilotageException.class, e -> {
            assertThat(e.status()).isEqualTo(400);
            assertThat(e.code()).isEqualTo("PIL_PERIODE_INVALIDE");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = { "2026-W00", "2026-W54", "2025-W53", "2026-41", "2026-10-06" })
    @DisplayName("Semaine mal formée ou inexistante : 400 PIL_PERIODE_INVALIDE")
    void semaineInvalide(String texte) {
        assertThatThrownBy(() -> Periode.de(TypeRapport.HEBDOMADAIRE, texte)).isInstanceOfSatisfying(PilotageException.class, e ->
            assertThat(e.code()).isEqualTo("PIL_PERIODE_INVALIDE")
        );
    }
}
