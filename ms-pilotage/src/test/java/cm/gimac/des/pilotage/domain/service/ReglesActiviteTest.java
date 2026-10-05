package cm.gimac.des.pilotage.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.pilotage.domain.activite.CategorieEvenement;
import cm.gimac.des.pilotage.domain.exception.PilotageException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReglesActiviteTest {

    @Test
    @DisplayName("Fuseau Africa/Douala : le jour change à 23:00 UTC ; horodatage en +01:00")
    void fuseau() {
        assertThat(ReglesActivite.aujourdhui(Clock.fixed(Instant.parse("2026-10-06T22:59:00Z"), ZoneOffset.UTC))).isEqualTo(LocalDate.of(2026, 10, 6));
        assertThat(ReglesActivite.aujourdhui(Clock.fixed(Instant.parse("2026-10-06T23:00:00Z"), ZoneOffset.UTC))).isEqualTo(LocalDate.of(2026, 10, 7));
        assertThat(ReglesActivite.debutJour(LocalDate.of(2026, 10, 6))).isEqualTo(Instant.parse("2026-10-05T23:00:00Z"));
        assertThat(ReglesActivite.horodatage(Instant.parse("2026-10-06T08:00:00Z"))).isEqualTo("2026-10-06T09:00:00+01:00");
        assertThat(ReglesActivite.lireHorodatage("debut", "2026-10-06T09:00:00+01:00")).isEqualTo(Instant.parse("2026-10-06T08:00:00Z"));
    }

    @Test
    @DisplayName("Bornes d'un événement : fin après début ; journée entière à minuit, fin exclusive")
    void bornes() {
        Instant debut = Instant.parse("2026-10-06T08:00:00Z");
        ReglesActivite.bornesEvenement(debut, debut.plusSeconds(60), false);
        assertThatThrownBy(() -> ReglesActivite.bornesEvenement(debut, debut, false)).isInstanceOf(PilotageException.class);
        Instant minuit = ReglesActivite.debutJour(LocalDate.of(2026, 10, 6));
        ReglesActivite.bornesEvenement(minuit, ReglesActivite.debutJour(LocalDate.of(2026, 10, 7)), true);
        assertThatThrownBy(() -> ReglesActivite.bornesEvenement(debut, ReglesActivite.debutJour(LocalDate.of(2026, 10, 7)), true)).isInstanceOfSatisfying(
            PilotageException.class,
            e -> assertThat(e.code()).isEqualTo("PIL_REQUETE_INVALIDE")
        );
    }

    @Test
    @DisplayName("Intervalles : du ≤ au et nombre de jours borné (400 PIL_INTERVALLE_INVALIDE)")
    void intervalles() {
        LocalDate d = LocalDate.of(2026, 10, 1);
        ReglesActivite.intervalle(d, d.plusDays(61), ReglesActivite.AGENDA_JOURS_MAX);
        assertThatThrownBy(() -> ReglesActivite.intervalle(d, d.plusDays(62), ReglesActivite.AGENDA_JOURS_MAX)).isInstanceOfSatisfying(PilotageException.class, e ->
            assertThat(e.code()).isEqualTo("PIL_INTERVALLE_INVALIDE")
        );
        assertThatThrownBy(() -> ReglesActivite.intervalle(d, d.minusDays(1), 31)).isInstanceOf(PilotageException.class);
    }

    @Test
    @DisplayName("Textes, durées, valeurs d'énumération, jours ouvrés, pagination")
    void champs() {
        assertThat(ReglesActivite.texteObligatoire("titre", "  Revue  ", 200)).isEqualTo("Revue");
        assertThatThrownBy(() -> ReglesActivite.texteObligatoire("titre", " ", 200)).isInstanceOf(PilotageException.class);
        assertThatThrownBy(() -> ReglesActivite.texteObligatoire("titre", "x".repeat(201), 200)).isInstanceOf(PilotageException.class);
        assertThat(ReglesActivite.texteFacultatif("lieu", " ", 200)).isNull();
        assertThat(ReglesActivite.duree(1440)).isEqualTo(1440);
        assertThatThrownBy(() -> ReglesActivite.duree(1441)).isInstanceOf(PilotageException.class);
        assertThatThrownBy(() -> ReglesActivite.duree(-1)).isInstanceOf(PilotageException.class);
        assertThat(ReglesActivite.valeur("categorie", CategorieEvenement.class, null, CategorieEvenement.AUTRE)).isEqualTo(CategorieEvenement.AUTRE);
        assertThatThrownBy(() -> ReglesActivite.valeur("categorie", CategorieEvenement.class, "FETE", null)).isInstanceOf(PilotageException.class);
        assertThat(ReglesActivite.jourOuvreSuivant(LocalDate.of(2026, 10, 9))).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(ReglesActivite.jourOuvreSuivant(LocalDate.of(2026, 10, 6))).isEqualTo(LocalDate.of(2026, 10, 7));
        assertThat(ReglesActivite.taille(1000)).isEqualTo(100);
        assertThat(ReglesActivite.taille(null)).isEqualTo(20);
        assertThat(ReglesActivite.page(-3)).isZero();
    }
}
