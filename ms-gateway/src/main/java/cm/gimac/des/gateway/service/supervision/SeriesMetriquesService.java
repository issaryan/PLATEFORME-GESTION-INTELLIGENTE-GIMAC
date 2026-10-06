package cm.gimac.des.gateway.service.supervision;

import cm.gimac.des.gateway.domain.supervision.MetriqueSerie;
import cm.gimac.des.gateway.domain.supervision.PeriodeSerie;
import cm.gimac.des.gateway.domain.supervision.SerieMetrique;
import cm.gimac.des.gateway.domain.supervision.SeriesMetriques;
import cm.gimac.des.gateway.domain.supervision.port.SourceSeries;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * Séries temporelles des métriques de la plateforme (graphiques de la supervision) : requête de la liste
 * fermée {@link MetriqueSerie}, fenêtre calée sur le pas, mesures manquantes rendues explicites (null),
 * séries triées de la plus chargée à la moins chargée. Prometheus injoignable : réponse « indisponible »,
 * jamais une erreur (la page garde le reste de la supervision).
 */
public class SeriesMetriquesService {

    private static final Logger LOG = LoggerFactory.getLogger(SeriesMetriquesService.class);

    /** Cible acceptée : nom de service, de conteneur ou instance d'hôte ; rien qui puisse altérer le PromQL. */
    static final Pattern CIBLE = Pattern.compile("^[A-Za-z0-9._:-]{1,120}$");

    private final SourceSeries source;
    private final String selecteurConteneurs;
    private final Clock horloge;

    /**
     * @param projetConteneurs projet Docker Compose de la plateforme (serveur partagé) ; vide : tous les conteneurs
     */
    public SeriesMetriquesService(SourceSeries source, String projetConteneurs, Clock horloge) {
        this.source = source;
        this.selecteurConteneurs = projetConteneurs == null || projetConteneurs.isBlank()
            ? "name!=\"\""
            : "name!=\"\",container_label_com_docker_compose_project=\"" + projetConteneurs.replace("\"", "") + "\"";
        this.horloge = horloge;
    }

    /**
     * @throws IllegalArgumentException cible invalide
     */
    public Mono<SeriesMetriques> series(MetriqueSerie metrique, PeriodeSerie periode, String cible) {
        String c = cible == null || cible.isBlank() ? null : cible.strip();
        if (c != null && !CIBLE.matcher(c).matches()) {
            throw new IllegalArgumentException("Cible invalide");
        }
        Duration pas = periode.pas();
        long pasMs = pas.toMillis();
        Instant fin = Instant.ofEpochMilli((horloge.millis() / pasMs) * pasMs);
        Instant debut = fin.minus(periode.duree());
        String promql = metrique.requete(selecteurConteneurs, c);
        return source
            .plage(promql, metrique.etiquette(), debut, fin, pas)
            .map(brutes -> new SeriesMetriques(metrique, metrique.unite(), periode, debut, fin, pas.toSeconds(), true, densifier(brutes, debut, fin, pasMs)))
            .onErrorResume(e -> {
                LOG.warn("Séries {} indisponibles : {}", metrique, e.toString());
                return Mono.just(new SeriesMetriques(metrique, metrique.unite(), periode, debut, fin, pas.toSeconds(), false, List.of()));
            });
    }

    /** Un point par pas, de debut à fin ; null là où Prometheus n'a rien rendu ; tri par moyenne décroissante. */
    static List<SerieMetrique> densifier(List<SerieMetrique> brutes, Instant debut, Instant fin, long pasMs) {
        List<SerieMetrique> series = new ArrayList<>();
        for (SerieMetrique s : brutes) {
            Map<Long, Double> parInstant = new HashMap<>();
            s.points().forEach(p -> parInstant.put(p.t(), p.v()));
            List<SerieMetrique.Point> points = new ArrayList<>();
            for (long t = debut.toEpochMilli(); t <= fin.toEpochMilli(); t += pasMs) {
                points.add(new SerieMetrique.Point(t, parInstant.get(t)));
            }
            series.add(new SerieMetrique(s.nom(), points));
        }
        series.sort(Comparator.comparingDouble(SeriesMetriquesService::moyenne).reversed().thenComparing(SerieMetrique::nom));
        return series;
    }

    /** Moyenne des mesures présentes ; une série sans mesure passe en dernier. */
    static double moyenne(SerieMetrique s) {
        return s.points().stream().filter(p -> p.v() != null).mapToDouble(SerieMetrique.Point::v).average().orElse(Double.NEGATIVE_INFINITY);
    }
}
