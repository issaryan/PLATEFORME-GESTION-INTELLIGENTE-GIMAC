package cm.gimac.des.iam.config;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.PREFERRED_USERNAME;

import cm.gimac.des.iam.infrastructure.security.ClientInterneAuthorizationManager;
import cm.gimac.des.iam.infrastructure.security.StrongAuthenticationAuthorizationManager;
import cm.gimac.des.iam.security.*;
import cm.gimac.des.iam.security.oauth2.AudienceValidator;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import tech.jhipster.config.JHipsterProperties;

@Configuration
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {

    private final JHipsterProperties jHipsterProperties;

    @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}")
    private String issuerUri;

    /** D-61 : jeton issu d'une authentification à deux facteurs, attestée par le claim acr. */
    @Value("${gimac.securite.double-facteur.exige:true}")
    private boolean doubleFacteurExige;

    @Value("${gimac.securite.double-facteur.niveaux-acr:2,3}")
    private Set<String> niveauxAcr;

    /**
     * D-65 : session restreinte du premier accès (octroi direct à un facteur, acr 1), admise sur les
     * seules routes de l'assistant ; tout le reste exige toujours acr 2 ou 3 (D-61).
     */
    @Value("${gimac.securite.double-facteur.niveaux-acr-premier-acces:1,2,3}")
    private Set<String> niveauxAcrPremierAcces;

    /** D-65 : clients de service admis sur les routes internes de la connexion (la passerelle). */
    @Value("${gimac.iam.connexion.clients-internes:gateway-service}")
    private Set<String> clientsInternesConnexion;

    public SecurityConfiguration(JHipsterProperties jHipsterProperties) {
        this.jHipsterProperties = jHipsterProperties;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz ->
                // prettier-ignore
                authz
                    .requestMatchers("/api/authenticate").permitAll()
                    .requestMatchers("/api/auth-info").permitAll()
                    .requestMatchers("/api/admin/**").hasAuthority(AuthoritiesConstants.ADMIN)
                    // Connexion sur la plateforme (D-65) : jeton de service de la passerelle seulement.
                    .requestMatchers("/api/v1/interne/comptes/*/etat-connexion", "/api/v1/interne/comptes/*/alerte-connexion", "/api/v1/interne/connexion/**")
                        .access(new ClientInterneAuthorizationManager("INTERNAL_IAM_READ", clientsInternesConnexion))
                    // Interfaces internes : jeton de service uniquement (PI §0.7).
                    .requestMatchers("/api/v1/interne/**").hasAuthority("INTERNAL_IAM_READ")
                    // Premier accès (D-65) : acr 1 admis sur les seules routes de l'assistant.
                    .requestMatchers(HttpMethod.GET, "/api/v1/moi").access(new StrongAuthenticationAuthorizationManager(doubleFacteurExige, niveauxAcrPremierAcces))
                    .requestMatchers(HttpMethod.POST, "/api/v1/moi/premiere-configuration", "/api/v1/moi/mot-de-passe")
                        .access(new StrongAuthenticationAuthorizationManager(doubleFacteurExige, niveauxAcrPremierAcces))
                    .requestMatchers(HttpMethod.POST, "/api/v1/moi/second-facteur/**")
                        .access(new StrongAuthenticationAuthorizationManager(doubleFacteurExige, niveauxAcrPremierAcces))
                    // Routes métier : jeton à deux facteurs (D-61), puis @PreAuthorize route par route.
                    .requestMatchers("/api/v1/**").access(new StrongAuthenticationAuthorizationManager(doubleFacteurExige, niveauxAcr))
                    // Ressources générées par JHipster : administration technique uniquement.
                    .requestMatchers("/api/**").hasAuthority(AuthoritiesConstants.ADMIN)
                    .requestMatchers("/v3/api-docs/**").hasAuthority(AuthoritiesConstants.ADMIN)
                    .requestMatchers("/management/health").permitAll()
                    .requestMatchers("/management/health/**").permitAll()
                    .requestMatchers("/management/info").permitAll()
                    .requestMatchers("/management/prometheus").permitAll()
                    .requestMatchers("/management/**").hasAuthority(AuthoritiesConstants.ADMIN)
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter())))
            .oauth2Client(withDefaults());
        return http.build();
    }

    Converter<Jwt, AbstractAuthenticationToken> authenticationConverter() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
            new Converter<Jwt, Collection<GrantedAuthority>>() {
                @Override
                public Collection<GrantedAuthority> convert(Jwt jwt) {
                    return SecurityUtils.extractAuthorityFromClaims(jwt.getClaims());
                }
            }
        );
        jwtAuthenticationConverter.setPrincipalClaimName(PREFERRED_USERNAME);
        return jwtAuthenticationConverter;
    }

    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromOidcIssuerLocation(issuerUri);

        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(jHipsterProperties.getSecurity().getOauth2().getAudience());
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);

        jwtDecoder.setJwtValidator(withAudience);

        return jwtDecoder;
    }
}
