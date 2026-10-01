package cm.gimac.des.gateway.web.filter;

import cm.gimac.des.gateway.domain.connexion.CauseEchec;
import cm.gimac.des.gateway.security.session.FermetureSession;
import cm.gimac.des.gateway.security.session.ReponseProbleme;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Refresh oauth2 tokens based on TokenRelayGatewayFilterFactory.
 * <p>
 * D-65 : si le rafraîchissement est refusé (session Keycloak fermée, jeton révoqué) ou si le client
 * autorisé manque, la session de la passerelle est fermée et la requête reçoit 401
 * CNX_SESSION_EXPIREE, au lieu d'une redirection vers le fournisseur d'identité.
 */
@Component
public class OAuth2ReactiveRefreshTokensWebFilter implements WebFilter {

    private static final String LOGOUT_PATH = "/api/logout";

    private final ReactiveOAuth2AuthorizedClientManager clientManager;
    private final ObjectProvider<FermetureSession> fermeture;

    public OAuth2ReactiveRefreshTokensWebFilter(ReactiveOAuth2AuthorizedClientManager clientManager) {
        this(clientManager, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public OAuth2ReactiveRefreshTokensWebFilter(ReactiveOAuth2AuthorizedClientManager clientManager, ObjectProvider<FermetureSession> fermeture) {
        this.clientManager = clientManager;
        this.fermeture = fermeture;
    }

    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (LOGOUT_PATH.equals(exchange.getRequest().getPath().value())) {
            return chain.filter(exchange);
        }

        return exchange
            .getPrincipal()
            .filter(principal -> principal instanceof OAuth2AuthenticationToken)
            .cast(OAuth2AuthenticationToken.class)
            .flatMap(authentication ->
                authorizedClient(exchange, authentication)
                    .thenReturn(Boolean.TRUE)
                    .onErrorResume(OAuth2AuthorizationException.class, e -> expirer(exchange).thenReturn(Boolean.FALSE))
            )
            .defaultIfEmpty(Boolean.TRUE)
            .flatMap(poursuivre -> Boolean.TRUE.equals(poursuivre) ? chain.filter(exchange) : Mono.empty());
    }

    /** Jetons inutilisables : session fermée, 401 CNX_SESSION_EXPIREE. */
    private Mono<Void> expirer(ServerWebExchange exchange) {
        FermetureSession f = fermeture == null ? null : fermeture.getIfAvailable();
        Mono<Void> fermer = exchange.getSession().flatMap(s -> f == null ? s.invalidate() : f.fermer(s, true));
        return fermer.then(ReponseProbleme.ecrire(exchange, CauseEchec.SESSION_EXPIREE));
    }

    private Mono<OAuth2AuthorizedClient> authorizedClient(ServerWebExchange exchange, OAuth2AuthenticationToken oauth2Authentication) {
        String clientRegistrationId = oauth2Authentication.getAuthorizedClientRegistrationId();
        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.withClientRegistrationId(clientRegistrationId)
            .principal(oauth2Authentication)
            .attribute(ServerWebExchange.class.getName(), exchange)
            .build();
        if (clientManager == null) {
            return Mono.error(
                new IllegalStateException(
                    "No ReactiveOAuth2AuthorizedClientManager bean was found. Did you include the " +
                        "org.springframework.boot:spring-boot-starter-oauth2-client dependency?"
                )
            );
        }
        return clientManager.authorize(request);
    }
}
