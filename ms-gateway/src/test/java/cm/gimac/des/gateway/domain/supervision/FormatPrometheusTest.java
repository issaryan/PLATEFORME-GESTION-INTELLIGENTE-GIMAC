package cm.gimac.des.gateway.domain.supervision;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.gateway.domain.supervision.FormatPrometheus.Echantillon;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Analyse du format texte de Prometheus et extraction des métriques d'exploitation (D-61). */
class FormatPrometheusTest {

    static final String EXPOSITION = """
        # HELP process_uptime_seconds The uptime of the Java virtual machine
        # TYPE process_uptime_seconds gauge
        process_uptime_seconds{application="participant"} 3600.5
        process_cpu_usage{application="participant"} 0.125
        system_cpu_usage{application="participant"} 0.5
        system_cpu_count{application="participant"} 8.0
        jvm_memory_used_bytes{application="participant",area="heap",id="G1 Eden Space"} 1.048576E8
        jvm_memory_used_bytes{application="participant",area="heap",id="G1 Old Gen"} 5.0E7
        jvm_memory_used_bytes{application="participant",area="nonheap",id="Metaspace"} 9.9E7
        jvm_memory_max_bytes{application="participant",area="heap",id="G1 Eden Space"} -1.0
        jvm_memory_max_bytes{application="participant",area="heap",id="G1 Old Gen"} 5.36870912E8
        disk_free_bytes{application="participant",path="/app/."} 1.0E10
        disk_total_bytes{application="participant",path="/app/."} 5.0E10
        system_load_average_1m{application="participant"} 1.75
        jvm_threads_live_threads{application="participant"} 42.0
        http_server_requests_seconds_count{uri="/api"} 12 1695980000000
        """;

    @Test
    @DisplayName("Extraction : durée, CPU, tas additionné par zone (max -1 ignoré), disque, charge, threads")
    void extraction() {
        List<String> lignes = EXPOSITION.lines().toList();
        Metriques m = FormatPrometheus.extraire(FormatPrometheus.analyser(lignes, FormatPrometheus.METRIQUES_EXPLOITATION::contains));
        assertThat(m.dureeFonctionnementSecondes()).isEqualTo(3600.5);
        assertThat(m.cpuProcessus()).isEqualTo(0.125);
        assertThat(m.cpuSysteme()).isEqualTo(0.5);
        assertThat(m.nombreCpu()).isEqualTo(8);
        assertThat(m.memoireTasUtiliseeOctets()).isEqualTo(1.548576E8);
        assertThat(m.memoireTasMaxOctets()).isEqualTo(5.36870912E8);
        assertThat(m.memoireTasPourcent()).isEqualTo(28.84);
        assertThat(m.disqueLibreOctets()).isEqualTo(1.0E10);
        assertThat(m.disqueTotalOctets()).isEqualTo(5.0E10);
        assertThat(m.chargeMoyenne1m()).isEqualTo(1.75);
        assertThat(m.threadsActifs()).isEqualTo(42);
    }

    @Test
    @DisplayName("Seules les métriques acceptées sont retenues ; horodatage final toléré")
    void filtre() {
        List<Echantillon> e = FormatPrometheus.analyser(EXPOSITION.lines().toList(), "http_server_requests_seconds_count"::equals);
        assertThat(e).singleElement().satisfies(x -> {
            assertThat(x.valeur()).isEqualTo(12.0);
            assertThat(x.etiquettes()).containsEntry("uri", "/api");
        });
    }

    @Test
    @DisplayName("Étiquettes : guillemets, barres obliques inverses et sauts de ligne échappés")
    void echappements() {
        Echantillon e = FormatPrometheus.analyserLigne("m{a=\"x\\\"y\",b=\"c\\\\d\",c=\"l1\\nl2\"} 3").orElseThrow();
        assertThat(e.etiquettes()).containsEntry("a", "x\"y").containsEntry("b", "c\\d").containsEntry("c", "l1\nl2");
        assertThat(FormatPrometheus.analyserLigne("m{} 1").orElseThrow().etiquettes()).isEmpty();
        assertThat(FormatPrometheus.analyserLigne("m 7").orElseThrow().valeur()).isEqualTo(7.0);
    }

    @Test
    @DisplayName("Valeurs spéciales : NaN, +Inf, -Inf ; valeurs non finies écartées de l'extraction")
    void valeursSpeciales() {
        assertThat(FormatPrometheus.valeur("NaN")).isNaN();
        assertThat(FormatPrometheus.valeur("+Inf")).isEqualTo(Double.POSITIVE_INFINITY);
        assertThat(FormatPrometheus.valeur("Inf")).isEqualTo(Double.POSITIVE_INFINITY);
        assertThat(FormatPrometheus.valeur("-Inf")).isEqualTo(Double.NEGATIVE_INFINITY);
        assertThat(FormatPrometheus.valeur("abc")).isNull();
        Metriques m = FormatPrometheus.extraire(
            FormatPrometheus.analyser(List.of("process_cpu_usage NaN", "jvm_memory_used_bytes{area=\"heap\"} 10"), n -> true)
        );
        assertThat(m.cpuProcessus()).isNull();
        assertThat(m.memoireTasUtiliseeOctets()).isEqualTo(10.0);
        assertThat(m.memoireTasMaxOctets()).isNull();
        assertThat(m.memoireTasPourcent()).isNull();
    }

    @Test
    @DisplayName("Lignes vides, commentaires et lignes mal formées ignorés")
    void lignesIgnorees() {
        assertThat(FormatPrometheus.nomMetrique(null)).isEmpty();
        assertThat(FormatPrometheus.nomMetrique("   ")).isEmpty();
        assertThat(FormatPrometheus.nomMetrique("# TYPE x gauge")).isEmpty();
        assertThat(FormatPrometheus.analyserLigne("")).isEmpty();
        assertThat(FormatPrometheus.analyserLigne("m{a=\"x\"")).isEmpty();
        assertThat(FormatPrometheus.analyserLigne("m{a=x} 1")).isEmpty();
        assertThat(FormatPrometheus.analyserLigne("m{a=\"x} 1")).isEmpty();
        assertThat(FormatPrometheus.analyserLigne("m{a=")).isEmpty();
        assertThat(FormatPrometheus.analyserLigne("m{a=\"x\"}")).isEmpty();
        assertThat(FormatPrometheus.analyserLigne("m pasunnombre")).isEmpty();
        assertThat(FormatPrometheus.analyser(List.of("", "# HELP", "m{"), n -> true)).isEmpty();
    }

    @Test
    @DisplayName("Aucune métrique : toutes les valeurs à null ; tas maximal nul sans pourcentage")
    void vide() {
        assertThat(FormatPrometheus.extraire(List.of())).isEqualTo(Metriques.VIDES);
        Metriques m = FormatPrometheus.extraire(
            FormatPrometheus.analyser(
                List.of("jvm_memory_used_bytes{area=\"heap\"} 10", "jvm_memory_max_bytes{area=\"heap\"} 0"),
                n -> true
            )
        );
        assertThat(m.memoireTasPourcent()).isNull();
    }
}
