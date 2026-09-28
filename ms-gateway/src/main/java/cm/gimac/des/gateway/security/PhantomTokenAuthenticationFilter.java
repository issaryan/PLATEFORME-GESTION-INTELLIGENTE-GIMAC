package cm.gimac.des.gateway.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.util.Objects;
import java.util.UUID;

/**
 * Filtre global reactif appliquant le pattern Phantom Token (INF-13).
 *
 * <p>
 * Intercepte les requetes entrantes, extrait le jeton opaque Bearer,
 * l'echange contre un JWT interne signe HS256 (via introspection Keycloak),
 * extrait le tenant_id, genere un correlation_id, et propage l'ensemble
 * en aval de maniere securisee.
 *
 * <p>
 * CORRECTION : Le {@code .onErrorResume} capture desormais toutes les
 * exceptions securite ({@link InvalidBearerTokenException}) en plus des
 * {@code UnauthorizedException} internes. Avant ce fix, un jeton invalide
 * (ex: alg=none) declenchait une {@link InvalidBearerTokenException} non
 * capturee qui remontait comme 500 au lieu de 401.
 */
@ConditionalOnProperty(name = "gimac.securite.jeton-fantome.actif", havingValue = "true")
@Component
public class PhantomTokenAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(PhantomTokenAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    private final KeycloakIntrospectionClient introspectionClient;

    public PhantomTokenAuthenticationFilter(KeycloakIntrospectionClient introspectionClient) {
        this.introspectionClient = Objects.requireNonNull(
                introspectionClient, "Le KeycloakIntrospectionClient ne peut pas etre nul");
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Objects.requireNonNull(exchange, "ServerWebExchange ne peut pas etre nul");
        Objects.requireNonNull(chain, "GatewayFilterChain ne peut pas etre nul");

        ServerHttpRequest request = exchange.getRequest();

        // Tracabilite transverse
        String path = request.getURI().getPath();
        if (path.matches(".*(/v3/api-docs|/actuator|/management|/swagger-ui|/openapi\\.json|/docs|/redoc).*")
                || "/api/internal/jwks".equals(path)
                // LA SONDE DU SECOND FACTEUR PASSE SANS JETON, par necessite : la page de
                // connexion demande s'il faut un code AVANT d'etre authentifiee. Keycloak
                // repondant « Invalid user credentials » aussi bien pour un mot de passe
                // faux que pour un code manquant, l'ecran ne peut pas trancher seul.
                //
                // Ce que la sonde divulgue est reduit au minimum : un booleen, et un login
                // inconnu rend `false` comme un compte sans second facteur — elle ne sert
                // donc pas a enumerer les comptes.
                || "/api/v1/utilisateurs/keycloak/second-facteur".equals(path)) {
            return chain.filter(exchange);
        }

        // Requete deja authentifiee par cle API (ApiKeyAuthenticationFilter, order -110) : celui-ci
        // a deja minte un JWT interne et pose l'en-tete Authorization → ne pas re-introspecter.
        if (Boolean.TRUE.equals(exchange.getAttribute(
                "gimac.cleApi"))) {
            return chain.filter(exchange);
        }

        String correlationId = request.getHeaders().getFirst(CORRELATION_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        final String finalCorrelationId = correlationId;

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // Absence ou malformation de l'en-tete Authorization → 401 immediat
        if (authHeader == null
                || !authHeader.startsWith(BEARER_PREFIX)
                || authHeader.length() <= BEARER_PREFIX.length()) {
            log.warn("[{}] Requete rejetee : En-tete Authorization manquant, malforme ou vide.",
                    finalCorrelationId);
            return Mono.error(new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED));
        }

        String opaqueToken = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (opaqueToken.isBlank()) {
            log.warn("[{}] Requete rejetee : Jeton Bearer vide.", finalCorrelationId);
            return Mono.error(new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED));
        }

        return introspectionClient.exchangeOpaqueForJwt(opaqueToken)
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("[{}] Jeton opaque invalide ou expire.", finalCorrelationId);
                    return Mono.error(new UnauthorizedException());
                }))
                .flatMap(jwt -> {
                    try {
                        SignedJWT parsedJwt = (SignedJWT) JWTParser.parse(jwt);
                        Object tenantClaim = parsedJwt.getJWTClaimsSet().getClaim("tenant_id");
                        // tenant_id est facultatif : utilisé pour la classification uniquement,
                        // pas comme critère de ségrégation strict.
                        String tenantId = (tenantClaim != null && !tenantClaim.toString().isBlank())
                                ? tenantClaim.toString() : null;

                        if (tenantId != null) {
                            log.debug("[{}] Claim tenant_id présent : {}", finalCorrelationId, tenantId);
                        }

                        // SECURITE : headers.set() ecrase toute tentative d'injection
                        // de X-Tenant-Id par le client externe (HTTP Header Smuggling)
                        ServerHttpRequest mutatedRequest = request.mutate()
                                .headers(headers -> {
                                    headers.set(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + jwt);
                                    if (tenantId != null) {
                                        headers.set(TENANT_HEADER, tenantId);
                                    } else {
                                        headers.remove(TENANT_HEADER);
                                    }
                                    headers.set(CORRELATION_HEADER, finalCorrelationId);
                                })
                                .build();

                        return Mono.just(exchange.mutate().request(mutatedRequest).build());

                    } catch (ParseException e) {
                        log.error("[{}] Echec de la validation ou jeton non reconnu (Parsing JWT impossible).",
                                finalCorrelationId, e);
                        return Mono.error(new UnauthorizedException());
                    }
                })
                // CORRECTION : capture InvalidBearerTokenException en plus de
                // UnauthorizedException.
                // Avant ce fix : un token invalide (alg=none, expired, etc.) levait
                // InvalidBearerTokenException depuis KeycloakIntrospectionClient → non capturee
                // → propagee comme 500. Desormais toute exception securite → 401.
                .onErrorResume(UnauthorizedException.class, e -> Mono.error(new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED)))
                .onErrorResume(InvalidBearerTokenException.class, e -> {
                    log.warn("[{}] Jeton invalide ou refuse par le fournisseur d'identite : {}",
                            finalCorrelationId, e.getMessage());
                    return Mono.error(new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED));
                })
                // Filet de securite : toute RuntimeException non prevue → 401
                // (pas de fuite d'information sur l'architecture interne)
                .onErrorResume(RuntimeException.class, e -> {
                    log.error("[{}] Erreur d'introspection dans le filtre Phantom Token : {}",
                            finalCorrelationId, e.getMessage());
                    return Mono.error(new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED));
                })
                .flatMap(mutatedExchange -> chain.filter(mutatedExchange));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private static class UnauthorizedException extends RuntimeException {
    }
}