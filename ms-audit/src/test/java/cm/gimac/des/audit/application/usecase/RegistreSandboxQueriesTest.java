package cm.gimac.des.audit.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/** US-EXI-10 : bornes de pagination, filtre d'institution, champs CSV (RFC 4180), nom du fichier d'export. */
class RegistreSandboxQueriesTest {

    @Test
    @DisplayName("Pages de 1 à 100, institution vide ignorée, champ CSV entre guillemets si nécessaire, suffixe du fichier")
    void outils() {
        assertThat(RegistreSandboxQueries.taille(0)).isEqualTo(1);
        assertThat(RegistreSandboxQueries.taille(500)).isEqualTo(100);
        assertThat(RegistreSandboxQueries.institution(" ")).isNull();
        assertThat(RegistreSandboxQueries.institution(null)).isNull();
        assertThat(RegistreSandboxQueries.institution(" 99001 ")).isEqualTo("99001");
        assertThat(RegistreSandboxQueries.csv(null)).isEmpty();
        assertThat(RegistreSandboxQueries.csv("simple")).isEqualTo("simple");
        assertThat(RegistreSandboxQueries.csv("a,b")).isEqualTo("\"a,b\"");
        assertThat(RegistreSandboxQueries.csv("dit \"oui\"")).isEqualTo("\"dit \"\"oui\"\"\"");
        assertThat(RegistreSandboxQueries.csv("l1\nl2")).isEqualTo("\"l1\nl2\"");
        assertThat(RegistreSandboxQueries.csv("l1\rl2")).isEqualTo("\"l1\rl2\"");
        assertThat(RegistreSandboxQueries.institutionAffichee(null)).isEmpty();
        assertThat(RegistreSandboxQueries.institutionAffichee("99 001")).isEqualTo("-99_001");
        assertThat(RegistreSandboxQueries.instant(null)).isNull();
        assertThat(RegistreSandboxQueries.instant(Timestamp.from(Instant.EPOCH))).isEqualTo(Instant.EPOCH);
    }

    @Test
    @DisplayName("Rebut sous le seuil d'alerte (AUD-07) : aucune alerte ; compte absent lu comme zéro")
    void rebutSousLeSeuil() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        var page = new RegistreSandboxQueries(jdbc, 10).rebut(-1, 20);
        assertThat(page.total()).isZero();
        assertThat(page.alerte()).isFalse();
    }
}
