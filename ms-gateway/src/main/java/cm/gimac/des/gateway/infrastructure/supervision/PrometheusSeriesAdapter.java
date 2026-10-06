package cm.gimac.des.gateway.infrastructure.supervision;

import static cm.gimac.des.gateway.infrastructure.supervision.ClientsSupervision.carte;
import static cm.gimac.des.gateway.infrastructure.supervision.ClientsSupervision.liste;
import static cm.gimac.des.gateway.infrastructure.supervision.ClientsSupervision.nombre;

import cm.gimac.des.gateway.domain.supervision.SerieMetrique;
import cm.gimac.des.gateway.domain.supervision.port.SourceSeries;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Adaptateur WebClient du port {@link SourceSeries} : requête de plage de Prometheus (/api/v1/query_range),
 * réponse de type « matrix » lue avec tolérance (valeurs non numériques ou « NaN » ignorées).
 */
public class PrometheusSeriesAdapter implements SourceSeries {

    /** Nom de la série unique d'un agrégat sans étiquette (charge de toute la plateforme). */
    public static final String PLATEFORME = "plateforme";

    private final WebClient client;

    /** @param client client dont l'adresse de base est celle de Prometheus */
    public PrometheusSeriesAdapter(WebClient client) {
        this.client = client;
    }

    @Override
    public Mono<List<SerieMetrique>> plage(String promql, String etiquette, Instant debut, Instant fin, Duration pas) {
        return client
            .get()
            .uri(b ->
                b
                    .path("/api/v1/query_range")
                    .queryParam("query", "{q}")
                    .queryParam("start", debut.getEpochSecond())
                    .queryParam("end", fin.getEpochSecond())
                    .queryParam("step", pas.toSeconds())
                    .build(promql)
            )
            .retrieve()
            .bodyToMono(ActuatorWebClientAdapter.CARTE)
            .map(corps -> lireMatrice(corps, etiquette));
    }

    static List<SerieMetrique> lireMatrice(Map<String, Object> corps, String etiquette) {
        List<SerieMetrique> series = new ArrayList<>();
        for (Object o : liste(carte(corps.get("data")).get("result"))) {
            Map<String, Object> serie = carte(o);
            Object cle = etiquette == null ? PLATEFORME : carte(serie.get("metric")).get(etiquette);
            if (cle == null) {
                continue;
            }
            List<SerieMetrique.Point> points = new ArrayList<>();
            for (Object valeur : liste(serie.get("values"))) {
                List<?> paire = liste(valeur);
                Double t = paire.size() == 2 ? nombre(paire.get(0)) : null;
                Double v = paire.size() == 2 ? nombre(paire.get(1)) : null;
                if (t != null && v != null && !v.isNaN() && !v.isInfinite()) {
                    points.add(new SerieMetrique.Point(Math.round(t * 1000), v));
                }
            }
            series.add(new SerieMetrique(cle.toString(), points));
        }
        return series;
    }
}
