package cm.gimac.des.monitoring.infrastructure.client;

import cm.gimac.des.monitoring.application.port.out.DisponibilitesPort;
import cm.gimac.des.monitoring.domain.exception.MonitoringException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptateur réel des disponibilités (BF-ADMIN) : GET /api/v1/interne/disponibilites?du=&au= de MS-IAM (jeton de
 * service monitoring-service, INTERNAL_IAM_READ), congés et absences validés qui chevauchent la période. MS-IAM
 * injoignable : 503 MON_DISPONIBILITES_INDISPONIBLES, aucune grille n'est générée sans elles.
 */
@Component
@ConditionalOnProperty(name = "gimac.adaptateurs.disponibilites", havingValue = "real")
public class DisponibilitesIamAdapter implements DisponibilitesPort {

    record DisponibiliteIam(UUID id, String compteId, Instant debut, Instant fin, Boolean annulee) {}

    private static final ParameterizedTypeReference<List<DisponibiliteIam>> LISTE = new ParameterizedTypeReference<>() {};

    private final RestClient client;

    public DisponibilitesIamAdapter(RestClient.Builder builder, ServiceTokenInterceptor jeton, @Value("${gimac.services.iam.url}") String url) {
        this.client = builder.baseUrl(url).requestInterceptor(jeton).build();
    }

    @Override
    public List<Absence> absencesValidees(Instant du, Instant au) {
        try {
            List<DisponibiliteIam> l = client
                .get()
                .uri(u -> u.path("/api/v1/interne/disponibilites").queryParam("du", du.toString()).queryParam("au", au.toString()).build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(LISTE);
            return Optional.ofNullable(l)
                .orElse(List.of())
                .stream()
                .filter(d -> d.id() != null && d.compteId() != null && d.debut() != null && d.fin() != null)
                .map(d -> new Absence(d.id(), d.compteId(), d.debut(), d.fin(), Boolean.TRUE.equals(d.annulee())))
                .toList();
        } catch (RestClientException | IllegalStateException e) {
            throw MonitoringException.indisponible("MON_DISPONIBILITES_INDISPONIBLES", "Disponibilités (MS-IAM) indisponibles");
        }
    }
}
