package cm.gimac.des.gateway.infrastructure.supervision;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/** Construction des clients HTTP de la supervision et lecture tolérante des réponses JSON. */
public final class ClientsSupervision {

    /** Taille maximale d'une réponse lue en mémoire (environnement, loggers). */
    static final int TAILLE_MAX = 4 * 1024 * 1024;

    private ClientsSupervision() {}

    /**
     * Client au délai de connexion et de réponse bornés. Spring Boot 4 ne fournit pas de
     * WebClient.Builder dans ce contexte : le client est construit ici.
     */
    public static WebClient webClient(Duration delai, String baseUrl) {
        HttpClient http = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) Math.max(1, delai.toMillis()))
            .responseTimeout(delai);
        WebClient.Builder builder = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(http))
            .codecs(c -> c.defaultCodecs().maxInMemorySize(TAILLE_MAX));
        if (baseUrl != null && !baseUrl.isBlank()) {
            builder.baseUrl(sansBarreFinale(baseUrl));
        }
        return builder.build();
    }

    static String sansBarreFinale(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> carte(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    static List<?> liste(Object o) {
        return o instanceof List<?> l ? l : List.of();
    }

    static String texte(Object o) {
        return o == null ? null : o.toString();
    }

    static Double nombre(Object o) {
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        if (o == null) {
            return null;
        }
        try {
            double v = Double.parseDouble(o.toString());
            return Double.isFinite(v) ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Date RFC 3339 ; null si absente, illisible ou « jamais » (année 1 de Prometheus). */
    static Instant instant(Object o) {
        if (o == null) {
            return null;
        }
        try {
            OffsetDateTime d = OffsetDateTime.parse(o.toString());
            return d.getYear() <= 1 ? null : d.toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
