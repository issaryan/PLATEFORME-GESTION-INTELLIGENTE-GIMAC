package cm.gimac.des.gateway.config;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.PREFERRED_USERNAME;

import cm.gimac.des.gateway.security.AuthoritiesConstants;
import cm.gimac.des.gateway.security.DoubleFacteurAuthorizationManager;
import cm.gimac.des.gateway.security.SecurityUtils;
import cm.gimac.des.gateway.security.oauth2.AudienceValidator;
import cm.gimac.des.gateway.security.oauth2.EnrichissementJeton;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter.Mode;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.jhipster.config.JHipsterProperties;
import tech.jhipster.web.filter.reactive.CookieCsrfFilter;

@Configuration
@EnableReactiveMethodSecurity
public class SecurityConfiguration {

    private final JHipsterProperties jHipsterProperties;

    /** Niveaux d'assurance (acr) reconnus comme authentification à deux facteurs (D-61). */
    @Value("${gimac.securite.double-facteur.niveaux-acr:2,3}")
    private Set<String> niveauxDoubleFacteur = Set.of("2", "3");

    // See https://github.com/jhipster/generator-jhipster/issues/18868
    // We don't use a distributed cache or the user selected cache implementation here on purpose
    private final Cache<String, Mono<Jwt>> users = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofHours(1))
        .recordStats()
        .build();

    public SecurityConfiguration(JHipsterProperties jHipsterProperties) {
        this.jHipsterProperties = jHipsterProperties;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http.cors(withDefaults())
            .csrf(csrf ->
                csrf
                    .csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse())
                    // See https://stackoverflow.com/q/74447118/65681
                    .csrfTokenRequestHandler(new ServerCsrfTokenRequestAttributeHandler())
            )
            // See https://github.com/spring-projects/spring-security/issues/5766
            .addFilterAt(new CookieCsrfFilter(), SecurityWebFiltersOrder.REACTOR_CONTEXT)
            .headers(headers ->
                headers
                    .contentSecurityPolicy(csp -> csp.policyDirectives(jHipsterProperties.getSecurity().getContentSecurityPolicy()))
                    .frameOptions(frameOptions -> frameOptions.mode(Mode.SAMEORIGIN))
                    .referrerPolicy(referrer ->
                        referrer.policy(ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                    )
                    .permissionsPolicy(permissions ->
                        permissions.policy(
                            "camera=(), fullscreen=(self), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), midi=(), payment=(), sync-xhr=()"
                        )
                    )
            )
            .authorizeExchange(authz ->
                // prettier-ignore
                authz
                    // Connexion sur la plateforme (D-65) : publique, protégée contre la CSRF.
                    .pathMatchers("/api/connexion", "/api/connexion/**").permitAll()
                    // Délégation par redirection vers Keycloak retirée (D-65) : aucun traitement, 404.
                    .pathMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                    .pathMatchers("/api/authenticate").permitAll()
                    .pathMatchers("/api/auth-info").permitAll()
                    .pathMatchers("/api/admin/**").hasAuthority(AuthoritiesConstants.ADMIN)
                    // Supervision de la plateforme (D-61) : session à deux facteurs, puis permission par route (@PreAuthorize).
                    .pathMatchers("/api/plateforme/**").access(new DoubleFacteurAuthorizationManager(niveauxDoubleFacteur))
                    // Appareils connectés et historique des connexions (mon-compte.md §3) : session à deux facteurs.
                    .pathMatchers("/api/session/appareils", "/api/session/appareils/**", "/api/session/historique")
                        .access(new DoubleFacteurAuthorizationManager(niveauxDoubleFacteur))
                    .pathMatchers("/api/**").authenticated()
                    // Routes JHipster /services/{service}/** retirées (application.yml) : elles relayaient le CRUD
                    // généré des services hors OPA. Fermé à tous, même à ROLE_ADMIN, si une route y revenait.
                    .pathMatchers("/services", "/services/**").denyAll()
                    // Canal poussé des couches (contrat de messagerie §5) : servi par la passerelle, hors OPA ;
                    // session à deux facteurs exigée comme sur toute route /bff (D-61).
                    .pathMatchers(HttpMethod.GET, "/bff/*/live").access(new DoubleFacteurAuthorizationManager(niveauxDoubleFacteur))
                    // Présence des comptes internes (D-72) : servie par la passerelle avec le canal, mêmes exigences.
                    .pathMatchers(HttpMethod.GET, "/bff/*/presence").access(new DoubleFacteurAuthorizationManager(niveauxDoubleFacteur))
                    // Couches de présentation du lot 1 (PI-GIMAC-01 §0.6) : authentification exigée, puis OPA.
                    .pathMatchers("/bff/**").authenticated()
                    .pathMatchers("/v3/api-docs/**").hasAuthority(AuthoritiesConstants.ADMIN)
                    .pathMatchers("/management/health").permitAll()
                    .pathMatchers("/management/health/**").permitAll()
                    .pathMatchers("/management/info").permitAll()
                    .pathMatchers("/management/prometheus").permitAll()
                    .pathMatchers("/management/**").hasAuthority(AuthoritiesConstants.ADMIN)
            )
            // D-65 : ni oauth2Login ni oauth2Client (plus de redirection vers le fournisseur d'identité) ;
            // la session est ouverte par POST /api/connexion (octroi direct de serveur à serveur).
            .exceptionHandling(exceptionHandling -> exceptionHandling.authenticationEntryPoint(authenticationEntryPoint()))
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    /** Sans session : 401, jamais de redirection (l'écran de connexion est celui de la couche). */
    private ServerAuthenticationEntryPoint authenticationEntryPoint() {
        return new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED);
    }

    public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        ReactiveJwtAuthenticationConverter jwtAuthenticationConverter = new ReactiveJwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
            new Converter<Jwt, Flux<GrantedAuthority>>() {
                @Override
                public Flux<GrantedAuthority> convert(Jwt jwt) {
                    return Flux.fromIterable(SecurityUtils.extractAuthorityFromClaims(jwt.getClaims()));
                }
            }
        );
        jwtAuthenticationConverter.setPrincipalClaimName(PREFERRED_USERNAME);
        return jwtAuthenticationConverter;
    }

    @Bean
    ReactiveJwtDecoder jwtDecoder(ReactiveClientRegistrationRepository registrations) {
        Mono<ClientRegistration> clientRegistration = registrations.findByRegistrationId("oidc");

        return clientRegistration
            .map(oidc ->
                createJwtDecoder(
                    oidc.getProviderDetails().getIssuerUri(),
                    oidc.getProviderDetails().getJwkSetUri(),
                    oidc.getProviderDetails().getUserInfoEndpoint().getUri()
                )
            )
            .block();
    }

    private ReactiveJwtDecoder createJwtDecoder(String issuerUri, String jwkSetUri, String userInfoUri) {
        NimbusReactiveJwtDecoder jwtDecoder = new NimbusReactiveJwtDecoder(jwkSetUri);
        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(jHipsterProperties.getSecurity().getOauth2().getAudience());
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);

        jwtDecoder.setJwtValidator(withAudience);

        // Authentification d'un jeton porteur (comptes de service, supervision) : l'enrichissement par
        // userinfo n'en est jamais une condition (EnrichissementJeton).
        EnrichissementJeton enrichissement = new EnrichissementJeton(
            jeton ->
                WebClient.create()
                    .get()
                    .uri(userInfoUri)
                    .headers(headers -> headers.setBearerAuth(jeton))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {}),
            users,
            EnrichissementJeton.DELAI_USERINFO
        );
        return token -> jwtDecoder.decode(token).flatMap(enrichissement::enrichir);
    }
}
