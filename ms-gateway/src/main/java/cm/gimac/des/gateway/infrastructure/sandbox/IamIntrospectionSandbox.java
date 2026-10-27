package cm.gimac.des.gateway.infrastructure.sandbox;

import cm.gimac.des.gateway.domain.sandbox.port.IntrospectionSandbox;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/** Client du point d'introspection interne de MS-IAM, authentifié par le jeton de service gateway-service. */
public class IamIntrospectionSandbox implements IntrospectionSandbox {

    static final String CHEMIN = "/api/v1/interne/sandbox/introspection";
    private static final ParameterizedTypeReference<Map<String, Object>> JSON = new ParameterizedTypeReference<>() {};

    private final WebClient iam;
    private final Supplier<Mono<String>> jetonService;
    private final Duration delai;

    public IamIntrospectionSandbox(WebClient iam, Supplier<Mono<String>> jetonService, Duration delai) {
        this.iam = iam;
        this.jetonService = jetonService;
        this.delai = delai;
    }

    @Override
    public Mono<Portee> introspecter(String jeton, String institutionDemandee, String composant) {
        if (jeton == null || jeton.isBlank()) {
            return Mono.just(Portee.refusee());
        }
        return jetonService
            .get()
            .switchIfEmpty(Mono.error(() -> new IllegalStateException("Jeton de service gateway-service indisponible")))
            .flatMap(service ->
                iam
                    .post()
                    .uri(CHEMIN)
                    .headers(h -> h.setBearerAuth(service))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(new Requete(jeton, institutionDemandee, composant))
                    .retrieve()
                    .bodyToMono(JSON)
                    .map(IamIntrospectionSandbox::portee)
            )
            .timeout(delai);
    }

    @SuppressWarnings("unchecked")
    static Portee portee(Map<String, Object> corps) {
        if (!Boolean.TRUE.equals(corps.get("active"))) {
            return Portee.refusee();
        }
        Object permissions = corps.get("permissions");
        return new Portee(
            true,
            texte(corps.get("sub")),
            texte(corps.get("institution_id")),
            permissions instanceof List<?> l ? l.stream().map(String::valueOf).toList() : List.of(),
            texte(corps.get("type"))
        );
    }

    private static String texte(Object valeur) {
        return valeur == null ? null : valeur.toString();
    }

    /** Le jeton original est exclusivement transmis à MS-IAM et masqué dans toute représentation accidentelle. */
    record Requete(String jeton, String institution, String composant) {
        @Override
        public String toString() {
            return "Requete[jeton=****, institution=" + institution + ", composant=" + composant + "]";
        }
    }
}
