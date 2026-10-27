package cm.gimac.des.gateway.domain.sandbox.port;

import java.util.List;
import reactor.core.publisher.Mono;

/** Introspection à l'instant de la requête par MS-IAM, sans cache dans la passerelle. */
public interface IntrospectionSandbox {

    Mono<Portee> introspecter(String jeton, String institutionDemandee, String composant);

    /** Portée effective calculée par MS-IAM ; un refus ne révèle que {@code active = false}. */
    record Portee(boolean active, String sujet, String institution, List<String> permissions, String type) {
        public Portee {
            permissions = permissions == null ? List.of() : List.copyOf(permissions);
        }

        public static Portee refusee() {
            return new Portee(false, null, null, List.of(), null);
        }
    }
}
