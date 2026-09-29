package cm.gimac.des.gateway.web.filter;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Filtre « OpaAuthorize » des routes du lot 1 (PI-GIMAC-01 §0.6) : la passerelle soumet à OPA
 * la méthode, le chemin d'origine, les permissions et l'institution du jeton, et refuse (403)
 * toute requête que la politique gimac.authz n'autorise pas. OPA injoignable vaut refus
 * (refus par défaut, RG-24). Le service applique ensuite son propre @PreAuthorize.
 */
@Component
public class OpaAuthorizeGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private static final Logger LOG = LoggerFactory.getLogger(OpaAuthorizeGatewayFilterFactory.class);
    static final String INSTITUTION_CLAIM = "institution_id";

    private final WebClient opa;

    public OpaAuthorizeGatewayFilterFactory(@Value("${gimac.opa.url:http://localhost:8181}") String opaUrl) {
        super(Object.class);
        // Spring Boot 4 ne fournit pas de WebClient.Builder dans ce contexte : client construit ici.
        this.opa = WebClient.builder().baseUrl(opaUrl).build();
    }

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) ->
            ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .defaultIfEmpty(anonyme())
                .flatMap(auth -> decider(entree(exchange, auth)))
                .flatMap(autorise -> {
                    if (Boolean.TRUE.equals(autorise)) {
                        return chain.filter(exchange);
                    }
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                });
    }

    Map<String, Object> entree(ServerWebExchange exchange, Authentication auth) {
        boolean authentifie = auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousMarker);
        List<String> permissions = authentifie ? auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList() : List.of();
        return Map.of(
            "input",
            Map.of(
                "method",
                exchange.getRequest().getMethod().name(),
                "path",
                cheminOrigine(exchange),
                "permissions",
                permissions,
                "institution_id",
                authentifie ? institution(auth) : "",
                "authentifie",
                authentifie
            )
        );
    }

    /** Chemin reçu par la passerelle, avant réécriture éventuelle. */
    static String cheminOrigine(ServerWebExchange exchange) {
        Set<URI> origines = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR);
        if (origines != null && !origines.isEmpty()) {
            return origines.iterator().next().getPath();
        }
        return exchange.getRequest().getURI().getPath();
    }

    static String institution(Authentication auth) {
        Object valeur = null;
        if (auth instanceof JwtAuthenticationToken jwt) {
            valeur = jwt.getToken().getClaims().get(INSTITUTION_CLAIM);
        } else if (auth.getPrincipal() instanceof OidcUser oidc) {
            valeur = oidc.getClaims().get(INSTITUTION_CLAIM);
        }
        return valeur == null ? "" : valeur.toString();
    }

    private Mono<Boolean> decider(Map<String, Object> entree) {
        return opa
            .post()
            .uri("/v1/data/gimac/authz/allow")
            .bodyValue(entree)
            .retrieve()
            .bodyToMono(Map.class)
            .map(reponse -> Boolean.TRUE.equals(reponse.get("result")))
            .onErrorResume(e -> {
                LOG.warn("OPA injoignable, requête refusée : {}", e.getMessage());
                return Mono.just(false);
            });
    }

    private static Authentication anonyme() {
        return new AnonymousMarker();
    }

    /** Authentification vide : aucune permission, non authentifiée. */
    private static final class AnonymousMarker extends org.springframework.security.authentication.AbstractAuthenticationToken {

        AnonymousMarker() {
            super(List.of());
        }

        @Override
        public Object getCredentials() {
            return "";
        }

        @Override
        public Object getPrincipal() {
            return "anonyme";
        }
    }
}
