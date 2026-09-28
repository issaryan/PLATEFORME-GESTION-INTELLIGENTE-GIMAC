package cm.gimac.des.gateway.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nimbusds.jwt.JWTClaimsSet;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Client réactif responsable de l'échange de jetons opaques (Phantom Tokens)
 * contre des JWT signés via le point de terminaison d'introspection de
 * Keycloak.
 *
 * <p>
 * KEYCLOAK 25 COMPATIBILITY : Keycloak 25 ne supporte pas
 * {@code Accept: application/jwt} sur l'endpoint d'introspection. Il retourne
 * toujours un JSON contenant les claims du token. Ce client parse ce JSON,
 * vérifie {@code active: true}, extrait le {@code tenant_id}, puis construit
 * un JWT <b>RS256</b> signé localement avec la clé privée de la passerelle. Ce
 * JWT local est propagé en interne vers les microservices via le header
 * {@code Authorization: Bearer <jwt_local>}.
 *
 * <p>
 * <b>Pourquoi RS256 et non HS256.</b> Une signature symétrique impose de
 * distribuer la clé de signature à chacun des services qui vérifient le jeton.
 * Chacun d'eux peut alors en <i>forger</i> un, portant n'importe quel rôle —
 * {@code ROLE_SUPER_ADMIN} compris. Le motif Phantom Token repose pourtant sur
 * une hypothèse contraire : la passerelle est l'unique émetteur. Avec RS256, la
 * clé privée ne quitte jamais la passerelle et les services ne reçoivent que la
 * clé publique, via {@code /api/internal/jwks}.
 *
 * <p>
 * Implémente un cache Caffeine asynchrone pour absorber les pics de charge
 * (ex: 1000 req/s) sans subir l'effet de "Cache Stampede".
 */
@ConditionalOnProperty(name = "gimac.securite.jeton-fantome.actif", havingValue = "true")
@Component
public class KeycloakIntrospectionClient {

    private static final Logger log = LoggerFactory.getLogger(KeycloakIntrospectionClient.class);

    private final WebClient webClient;

    @NonNull
    private final String introspectionUri;

    @NonNull
    private final String clientId;

    @NonNull
    private final String clientSecret;

    /**
     * Unique point de signature des jetons internes, partagé avec le chemin
     * d'authentification par clé API. Voir {@link EmetteurJwtInterne}.
     */
    @NonNull
    private final EmetteurJwtInterne emetteur;

    private final AsyncLoadingCache<String, String> tokenCache;

    public KeycloakIntrospectionClient(
            WebClient.Builder webClientBuilder,
            @Value("${spring.security.oauth2.resourceserver.opaquetoken.introspection-uri}") String introspectionUri,
            @Value("${spring.security.oauth2.resourceserver.opaquetoken.client-id}") String clientId,
            @Value("${spring.security.oauth2.resourceserver.opaquetoken.client-secret}") String clientSecret,
            EmetteurJwtInterne emetteur) {
        this.webClient = webClientBuilder.build();
        this.introspectionUri = Objects.requireNonNull(introspectionUri,
                "L'URI d'introspection ne peut pas être nulle");
        this.clientId = Objects.requireNonNull(clientId, "Le Client ID ne peut pas être nul");
        this.clientSecret = Objects.requireNonNull(clientSecret, "Le Client Secret ne peut pas être nul");
        this.emetteur = Objects.requireNonNull(emetteur, "L'émetteur de JWT interne ne peut pas être nul");

        this.tokenCache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(4))
                .buildAsync((token, executor) -> fetchJwtFromKeycloak(Objects.requireNonNull(token)).toFuture());
    }

    /**
     * Point d'entrée appelé par {@link PhantomTokenAuthenticationFilter}.
     * Retourne un JWT RS256 signé localement, construit à partir des claims
     * validés par Keycloak via l'introspection.
     */
    public Mono<String> exchangeOpaqueForJwt(String opaqueToken) {
        if (opaqueToken == null || opaqueToken.isBlank()) {
            return Mono.error(new InvalidBearerTokenException("Jeton opaque manquant ou vide"));
        }
        CompletableFuture<String> cachedFuture = tokenCache.get(opaqueToken);
        return Mono.fromFuture(cachedFuture);
    }

    /**
     * Appel HTTP réel vers Keycloak (introspection JSON) + construction du JWT
     * interne.
     */
    private Mono<String> fetchJwtFromKeycloak(@NonNull String opaqueToken) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("token", opaqueToken);

        return webClient
                .post()
                .uri(this.introspectionUri)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .headers(headers -> headers.setBasicAuth(this.clientId, this.clientSecret))
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .timeout(Duration.ofMillis(800))
                .retryWhen(Retry.backoff(1, Duration.ofMillis(50))
                        .filter(t -> t instanceof TimeoutException))
                .flatMap(claims -> {
                    Object active = claims.get("active");
                    if (active == null || !Boolean.TRUE.equals(active)) {
                        log.warn("Jeton inactif ou expiré selon Keycloak (active={})", active);
                        return Mono.error(new InvalidBearerTokenException("Jeton inactif ou expiré"));
                    }

                    String tenantId = extractTenantId(claims);
                    String sub = (String) claims.getOrDefault("sub", "unknown");
                    String username = (String) claims.getOrDefault("preferred_username", sub);

                    if (tenantId == null || tenantId.isBlank()) {
                        log.warn("Claim tenant_id absent dans la réponse d'introspection pour sub={}", sub);
                    }

                    try {
                        String internalJwt = buildInternalJwt(sub, username, tenantId, claims);
                        log.debug("JWT interne construit pour sub={} tenant={}", sub, tenantId);
                        return Mono.just(internalJwt);
                    } catch (Exception e) {
                        log.error("Erreur lors de la construction du JWT interne", e);
                        return Mono.error(new RuntimeException("Impossible de construire le JWT interne", e));
                    }
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode().is4xxClientError()) {
                        log.warn("Jeton opaque invalide ou expiré (Keycloak a répondu {})", ex.getStatusCode());
                        return Mono.error(new InvalidBearerTokenException("Jeton invalide"));
                    }
                    log.error("Erreur serveur Keycloak lors de l'introspection : {}", ex.getMessage());
                    return Mono.error(new RuntimeException("Erreur serveur d'identité", ex));
                })
                .onErrorMap(TimeoutException.class, ex -> {
                    log.error("Timeout lors de l'introspection Keycloak (après retries)");
                    return new RuntimeException("Le serveur d'identité ne répond pas (Timeout)", ex);
                });
    }

    /**
     * Extrait le tenant_id depuis les claims d'introspection.
     * Keycloak mappe ce claim via un protocol mapper configuré dans
     * {@code jhipster-realm.json} (attribut de groupe).
     */
    @Nullable
    private String extractTenantId(Map<String, Object> claims) {
        // Chemin 1 : claim direct tenant_id (protocol mapper configuré)
        Object direct = claims.get("tenant_id");
        if (direct instanceof String s && !s.isBlank()) {
            return s;
        }

        // Chemin 2 : premier groupe de l'utilisateur (fallback)
        Object groups = claims.get("groups");
        if (groups instanceof List<?> groupList && !groupList.isEmpty()) {
            String firstGroup = groupList.get(0).toString();
            return firstGroup.startsWith("/") ? firstGroup.substring(1) : firstGroup;
        }

        return null;
    }

    /**
     * Assemble les claims issus de l'introspection, puis délègue la signature
     * RS256 à {@link EmetteurJwtInterne}. La clé privée ne quitte jamais la
     * passerelle ; les services vérifient via le JWKS public exposé à
     * {@code /api/internal/jwks}.
     */
    private String buildInternalJwt(
            String sub,
            String username,
            @Nullable String tenantId,
            Map<String, Object> keycloakClaims) throws Exception {
        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .subject(sub)
                .claim("preferred_username", username);

        if (tenantId != null && !tenantId.isBlank()) {
            claimsBuilder.claim("tenant_id", tenantId);
        }

        Object realmAccess = keycloakClaims.get("realm_access");
        if (realmAccess instanceof Map<?, ?> ra) {
            Object roles = ra.get("roles");
            if (roles != null) {
                claimsBuilder.claim("roles", roles);
            }
        }

        Object scope = keycloakClaims.get("scope");
        if (scope != null) {
            claimsBuilder.claim("scope", scope.toString());
        }

        return emetteur.emettre(claimsBuilder);
    }
}