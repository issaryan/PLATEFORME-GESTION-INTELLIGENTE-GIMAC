package cm.gimac.des.gateway.infrastructure.supervision;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import reactor.core.publisher.Mono;

/**
 * Jeton de service de la supervision : client OAuth2 « client_credentials » de l'enregistrement
 * « supervision » (client Keycloak gateway-supervision). Le gestionnaire met le jeton en cache et
 * le renouvelle à expiration. Sans client configuré ou joignable, le jeton est vide : la
 * supervision se poursuit en mode dégradé (santé sans détail, configuration non lue).
 */
public class JetonServiceSupervision implements Supplier<Mono<String>> {

    private static final Logger LOG = LoggerFactory.getLogger(JetonServiceSupervision.class);
    public static final String ENREGISTREMENT = "supervision";
    static final String PRINCIPAL = "gateway-supervision";

    private final ReactiveOAuth2AuthorizedClientManager gestionnaire;
    private final AtomicBoolean signale = new AtomicBoolean();

    public JetonServiceSupervision(ReactiveOAuth2AuthorizedClientManager gestionnaire) {
        this.gestionnaire = gestionnaire;
    }

    @Override
    public Mono<String> get() {
        if (gestionnaire == null) {
            return Mono.empty();
        }
        OAuth2AuthorizeRequest demande = OAuth2AuthorizeRequest.withClientRegistrationId(ENREGISTREMENT).principal(PRINCIPAL).build();
        return gestionnaire
            .authorize(demande)
            .map(client -> client.getAccessToken().getTokenValue())
            .doOnNext(j -> signale.set(false))
            .onErrorResume(e -> {
                // Signalé une fois par panne, pour ne pas inonder le journal au rythme du flux.
                if (signale.compareAndSet(false, true)) {
                    LOG.warn("Supervision : jeton de service indisponible ({}), mode dégradé", e.getMessage());
                }
                return Mono.empty();
            });
    }
}
