package cm.gimac.des.gateway.infrastructure.connexion;

import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import reactor.core.publisher.Mono;

/**
 * Jeton de service de la passerelle auprès de MS-IAM : client_credentials de l'enregistrement
 * « gateway-service » (client Keycloak gateway-service, permission INTERNAL_IAM_READ). Le
 * gestionnaire garde le jeton et ne le redemande qu'à son expiration. Vide s'il est indisponible.
 */
public class JetonServicePasserelle implements Supplier<Mono<String>> {

    private static final Logger LOG = LoggerFactory.getLogger(JetonServicePasserelle.class);
    public static final String ENREGISTREMENT = "gateway-service";

    private final ReactiveOAuth2AuthorizedClientManager gestionnaire;

    public JetonServicePasserelle(ReactiveOAuth2AuthorizedClientManager gestionnaire) {
        this.gestionnaire = gestionnaire;
    }

    @Override
    public Mono<String> get() {
        if (gestionnaire == null) {
            return Mono.empty();
        }
        OAuth2AuthorizeRequest demande = OAuth2AuthorizeRequest.withClientRegistrationId(ENREGISTREMENT).principal(ENREGISTREMENT).build();
        return gestionnaire
            .authorize(demande)
            .map(client -> client.getAccessToken().getTokenValue())
            .onErrorResume(e -> {
                LOG.warn("Jeton de service gateway-service indisponible : {}", e.getMessage());
                return Mono.empty();
            });
    }
}
