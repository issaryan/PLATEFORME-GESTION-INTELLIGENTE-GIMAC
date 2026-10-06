package cm.gimac.des.gateway.service.supervision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.gateway.domain.supervision.MetriqueSerie;
import cm.gimac.des.gateway.domain.supervision.PeriodeSerie;
import cm.gimac.des.gateway.domain.supervision.SerieMetrique;
import cm.gimac.des.gateway.domain.supervision.SeriesMetriques;
import cm.gimac.des.gateway.domain.supervision.port.SourceSeries;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

/** Séries des graphiques de la supervision : fenêtre, densification, tri, validation, indisponibilité. */
class SeriesMetriquesServiceTest {

    static final Instant MAINTENANT = Instant.parse("2026-10-06T10:00:07Z");
    static final Clock HORLOGE = Clock.fixed(MAINTENANT, ZoneOffset.UTC);

    /** Source simulée : mémorise la dernière demande et rend les séries fournies. */
    static class Source implements SourceSeries {

        String promql;
        String etiquette;
        Instant debut;
        Instant fin;
        Duration pas;
        Mono<List<SerieMetrique>> reponse = Mono.just(List.of());

        @Override
        public Mono<List<SerieMetrique>> plage(String promql, String etiquette, Instant debut, Instant fin, Duration pas) {
            this.promql = promql;
            this.etiquette = etiquette;
            this.debut = debut;
            this.fin = fin;
            this.pas = pas;
            return reponse;
        }
    }

    static SerieMetrique serie(String nom, long debutMs, Double... valeurs) {
        List<SerieMetrique.Point> points = new ArrayList<>();
        for (int i = 0; i < valeurs.length; i++) {
            if (valeurs[i] != null) {
                points.add(new SerieMetrique.Point(debutMs + i * 15_000L, valeurs[i]));
            }
        }
        return new SerieMetrique(nom, points);
    }

    @Test
    @DisplayName("Fenêtre calée sur le pas, requête de la liste fermée avec le projet et la cible")
    void fenetreEtRequete() {
        Source source = new Source();
        SeriesMetriques r = new SeriesMetriquesService(source, "gimac-des", HORLOGE).series(MetriqueSerie.CONTENEUR_CPU, PeriodeSerie.UNE_HEURE, " gimac-des-iam-1 ").block();
        assertThat(source.fin).isEqualTo(Instant.parse("2026-10-06T10:00:00Z"));
        assertThat(source.debut).isEqualTo(Instant.parse("2026-10-06T09:00:00Z"));
        assertThat(source.pas).isEqualTo(Duration.ofSeconds(15));
        assertThat(source.etiquette).isEqualTo("name");
        assertThat(source.promql).contains("container_label_com_docker_compose_project=\"gimac-des\"").contains("name=\"gimac-des-iam-1\"");
        assertThat(r.disponible()).isTrue();
        assertThat(r.unite()).isEqualTo("%");
        assertThat(r.pasSecondes()).isEqualTo(15);
        assertThat(r.metrique()).isEqualTo(MetriqueSerie.CONTENEUR_CPU);
        assertThat(r.periode()).isEqualTo(PeriodeSerie.UNE_HEURE);
    }

    @Test
    @DisplayName("Densification : un point par pas, null pour les mesures absentes ; tri par moyenne décroissante")
    void densificationEtTri() {
        Source source = new Source();
        long debut = Instant.parse("2026-10-06T09:00:00Z").toEpochMilli();
        source.reponse = Mono.just(List.of(serie("faible", debut, 1.0, 2.0), serie("forte", debut, 50.0, null, 70.0), serie("muette", debut)));
        SeriesMetriques r = new SeriesMetriquesService(source, "", HORLOGE).series(MetriqueSerie.SERVICE_DEBIT, PeriodeSerie.UNE_HEURE, null).block();
        assertThat(r.series()).extracting(SerieMetrique::nom).containsExactly("forte", "faible", "muette");
        SerieMetrique forte = r.series().get(0);
        assertThat(forte.points()).hasSize(241);
        assertThat(forte.points().get(0).v()).isEqualTo(50.0);
        assertThat(forte.points().get(1).v()).isNull();
        assertThat(forte.points().get(2).v()).isEqualTo(70.0);
        assertThat(forte.points().get(240).t()).isEqualTo(Instant.parse("2026-10-06T10:00:00Z").toEpochMilli());
        assertThat(r.series().get(2).points()).allSatisfy(p -> assertThat(p.v()).isNull());
        assertThat(source.promql).doesNotContain("compose_project");
    }

    @Test
    @DisplayName("Cible hors du jeu de caractères admis : refusée avant toute requête (aucune injection PromQL)")
    void cibleInvalide() {
        Source source = new Source();
        SeriesMetriquesService service = new SeriesMetriquesService(source, null, HORLOGE);
        assertThatThrownBy(() -> service.series(MetriqueSerie.SERVICE_DEBIT, PeriodeSerie.UNE_HEURE, "iam\"} or vector(1) #")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.series(MetriqueSerie.SERVICE_DEBIT, PeriodeSerie.UNE_HEURE, "a".repeat(121))).isInstanceOf(IllegalArgumentException.class);
        assertThat(source.promql).isNull();
        assertThat(service.series(MetriqueSerie.SERVICE_DEBIT, PeriodeSerie.UNE_HEURE, "  ").block().disponible()).isTrue();
    }

    @Test
    @DisplayName("Prometheus injoignable : réponse « indisponible » sans série, jamais une erreur")
    void indisponible() {
        Source source = new Source();
        source.reponse = Mono.error(new IllegalStateException("connexion refusée"));
        SeriesMetriques r = new SeriesMetriquesService(source, "", HORLOGE).series(MetriqueSerie.HOTE_CPU, PeriodeSerie.SEPT_JOURS, null).block();
        assertThat(r.disponible()).isFalse();
        assertThat(r.series()).isEmpty();
        assertThat(r.debut()).isEqualTo(r.fin().minus(Duration.ofDays(7)));
    }
}
