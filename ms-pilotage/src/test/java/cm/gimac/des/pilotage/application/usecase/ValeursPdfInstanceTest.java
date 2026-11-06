package cm.gimac.des.pilotage.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Valeurs lisibles du PDF d'une instance (D-139) : listes fermées traduites, cases, périodes, pièces, dates. */
class ValeursPdfInstanceTest {

    static final Map<String, String> FR = TextesPdfInstance.de("fr");
    static final Map<String, String> EN = TextesPdfInstance.de("en");
    static final Map<String, String> ES = TextesPdfInstance.de("es");

    @Test
    void listesFermees() {
        assertThat(ValeursPdfInstance.lisible(FR, "fr", "ACCEPTEE", true)).isEqualTo("Acceptée");
        assertThat(ValeursPdfInstance.lisible(EN, "en", "REFUSEE", true)).isEqualTo("Rejected");
        assertThat(ValeursPdfInstance.lisible(ES, "es", "ERREUR_CONNUE", true)).isEqualTo("Error conocido");
        assertThat(ValeursPdfInstance.choix("fr", "AUTRE_CODE_LIVRE")).isEqualTo("Autre code livre");
        assertThat(ValeursPdfInstance.choix("fr", "INCONNUE")).isEqualTo("Inconnue");
        // Valeurs saisies en clair ou sigles : inchangées.
        assertThat(ValeursPdfInstance.choix("en", "TCP")).isEqualTo("TCP");
        assertThat(ValeursPdfInstance.choix("en", "Accordée avec réserves")).isEqualTo("Accordée avec réserves");
        // Hors liste fermée, un code reste tel quel.
        assertThat(ValeursPdfInstance.lisible(FR, "fr", "ACCEPTEE", false)).isEqualTo("ACCEPTEE");
        assertThat(ValeursPdfInstance.lisible(EN, "en", List.of("NORMAL", "URGENT"), true)).isEqualTo("Normal, Urgent");
    }

    @Test
    void autresValeurs() {
        assertThat(ValeursPdfInstance.lisible(FR, "fr", null, true)).isEmpty();
        assertThat(ValeursPdfInstance.lisible(FR, "fr", true, false)).isEqualTo("Oui");
        assertThat(ValeursPdfInstance.lisible(ES, "es", false, false)).isEqualTo("No");
        assertThat(ValeursPdfInstance.lisible(EN, "en", true, false)).isEqualTo("Yes");
        assertThat(ValeursPdfInstance.lisible(FR, "fr", Map.of("debut", "2026-11-02", "fin", "2026-11-06"), false)).isEqualTo("02/11/2026 → 06/11/2026");
        assertThat(ValeursPdfInstance.lisible(FR, "fr", Map.of("documentId", "d1", "nomFichier", "ticket.pdf"), false)).isEqualTo("ticket.pdf");
        assertThat(ValeursPdfInstance.lisible(FR, "fr", Map.of("autre", "x"), false)).isEqualTo("{autre=x}");
        assertThat(ValeursPdfInstance.lisible(FR, "fr", Instant.parse("2026-10-28T09:00:00Z"), false)).isEqualTo("28/10/2026 10:00");
        assertThat(ValeursPdfInstance.lisible(FR, "fr", 12, false)).isEqualTo("12");
    }
}
