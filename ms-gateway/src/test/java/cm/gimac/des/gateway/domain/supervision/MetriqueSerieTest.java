package cm.gimac.des.gateway.domain.supervision;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Liste fermée des métriques historisées : requêtes PromQL produites, filtres et pas par période. */
class MetriqueSerieTest {

    static final String SEL = "name!=\"\",container_label_com_docker_compose_project=\"gimac-des\"";

    @Test
    @DisplayName("Conteneurs : sélecteur du projet, filtre facultatif sur le conteneur")
    void conteneurs() {
        assertThat(MetriqueSerie.PLATEFORME_CPU.requete(SEL, "ignoré")).isEqualTo("100 * sum(rate(container_cpu_usage_seconds_total{" + SEL + "}[2m]))");
        assertThat(MetriqueSerie.CONTENEUR_CPU.requete(SEL, null)).isEqualTo("100 * sum by (name) (rate(container_cpu_usage_seconds_total{" + SEL + "}[2m]))");
        assertThat(MetriqueSerie.CONTENEUR_MEMOIRE.requete(SEL, "gimac-des-iam-1"))
            .isEqualTo("sum by (name) (container_memory_working_set_bytes{" + SEL + ",name=\"gimac-des-iam-1\"}) / 1048576");
        assertThat(MetriqueSerie.PLATEFORME_CPU.etiquette()).isNull();
        assertThat(MetriqueSerie.CONTENEUR_MEMOIRE.unite()).isEqualTo("Mo");
    }

    @Test
    @DisplayName("Hôtes et services : filtre ajouté aux matchers existants ou en matcher seul")
    void hotesEtServices() {
        assertThat(MetriqueSerie.HOTE_CPU.requete(SEL, null)).contains("{mode=\"idle\"}").doesNotContain("F}");
        assertThat(MetriqueSerie.HOTE_CPU.requete(SEL, "node-exporter:9100")).contains("{mode=\"idle\",instance=\"node-exporter:9100\"}");
        assertThat(MetriqueSerie.HOTE_MEMOIRE.requete(SEL, null)).isEqualTo("100 * (1 - node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)");
        assertThat(MetriqueSerie.HOTE_MEMOIRE.requete(SEL, "h1")).contains("node_memory_MemAvailable_bytes{instance=\"h1\"}").contains("node_memory_MemTotal_bytes{instance=\"h1\"}");
        assertThat(MetriqueSerie.SERVICE_DEBIT.requete(SEL, "iam")).isEqualTo("sum by (application) (rate(http_server_requests_seconds_count{application=\"iam\"}[2m]))");
        assertThat(MetriqueSerie.SERVICE_ERREURS.requete(SEL, null)).contains("{status=~\"5..\"}").contains("rate(http_server_requests_seconds_count[5m])");
        assertThat(MetriqueSerie.SERVICE_LATENCE_P95.requete(SEL, "")).contains("histogram_quantile(0.95").contains("http_server_requests_seconds_bucket[5m]");
        assertThat(MetriqueSerie.SERVICE_TAS_JVM.requete(SEL, "gateway")).contains("{area=\"heap\",application=\"gateway\"}");
    }

    @ParameterizedTest
    @EnumSource(MetriqueSerie.class)
    @DisplayName("Aucun gabarit résiduel dans une requête produite")
    void sansGabarit(MetriqueSerie m) {
        for (String cible : new String[] { null, "x" }) {
            assertThat(m.requete(SEL, cible)).doesNotContain("{S").doesNotContain(",F}").doesNotContain("{F}");
        }
    }

    @Test
    @DisplayName("Pas : environ 240 points, jamais sous 15 s")
    void pas() {
        assertThat(PeriodeSerie.UNE_HEURE.pas()).isEqualTo(Duration.ofSeconds(15));
        assertThat(PeriodeSerie.SIX_HEURES.pas()).isEqualTo(Duration.ofSeconds(90));
        assertThat(PeriodeSerie.UN_JOUR.pas()).isEqualTo(Duration.ofMinutes(6));
        assertThat(PeriodeSerie.SEPT_JOURS.pas()).isEqualTo(Duration.ofMinutes(42));
        assertThat(PeriodeSerie.SEPT_JOURS.duree()).isEqualTo(Duration.ofDays(7));
    }
}
