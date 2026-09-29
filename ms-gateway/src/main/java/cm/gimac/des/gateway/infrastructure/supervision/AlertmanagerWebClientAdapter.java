package cm.gimac.des.gateway.infrastructure.supervision;

import static cm.gimac.des.gateway.infrastructure.supervision.ClientsSupervision.carte;
import static cm.gimac.des.gateway.infrastructure.supervision.ClientsSupervision.texte;

import cm.gimac.des.gateway.domain.supervision.Alerte;
import cm.gimac.des.gateway.domain.supervision.port.SourceAlertes;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/** Adaptateur WebClient du port {@link SourceAlertes} : API v2 d'Alertmanager, alertes actives non silencieuses. */
public class AlertmanagerWebClientAdapter implements SourceAlertes {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTE = new ParameterizedTypeReference<>() {};

    private final WebClient client;

    /** @param client client dont l'adresse de base est celle d'Alertmanager */
    public AlertmanagerWebClientAdapter(WebClient client) {
        this.client = client;
    }

    @Override
    public Mono<List<Alerte>> alertesActives() {
        return client
            .get()
            .uri(b -> b.path("/api/v2/alerts").queryParam("active", "true").queryParam("silenced", "false").queryParam("inhibited", "false").build())
            .retrieve()
            .bodyToMono(LISTE)
            .map(AlertmanagerWebClientAdapter::lireAlertes);
    }

    /** Alertes les plus récentes d'abord. */
    static List<Alerte> lireAlertes(List<Map<String, Object>> corps) {
        return corps
            .stream()
            .map(a -> {
                Map<String, Object> etiquettes = carte(a.get("labels"));
                Map<String, Object> annotations = carte(a.get("annotations"));
                return new Alerte(
                    texte(etiquettes.get("alertname")),
                    texte(etiquettes.get("severity")),
                    texte(annotations.get("summary")),
                    texte(annotations.get("description")),
                    ClientsSupervision.instant(a.get("startsAt")),
                    texte(etiquettes.get("instance")),
                    texte(etiquettes.get("job")),
                    texte(carte(a.get("status")).get("state"))
                );
            })
            .sorted(Comparator.comparing(Alerte::depuis, Comparator.nullsLast(Comparator.<Instant>reverseOrder())))
            .toList();
    }
}
