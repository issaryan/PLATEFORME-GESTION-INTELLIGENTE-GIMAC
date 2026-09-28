package cm.gimac.des.iam.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

/**
 * UTI-01 — Fournit explicitement le {@link ClientRegistrationRepository} de production,
 * SANS passer par la découverte OIDC ({@code ClientRegistrations.fromIssuerLocation}).
 * ────────────────────────────────────────────────────────────────────────────
 * Keycloak est configuré avec {@code KC_HOSTNAME=localhost} (infrastructure partagée,
 * non modifiée ici) : son document de découverte annonce toujours
 * {@code issuer=http://localhost:9080/...}, quel que soit l'hôte utilisé pour l'atteindre.
 * Or les services internes joignent Keycloak via {@code keycloak:9080} (réseau Docker) —
 * la validation stricte "issuer découvert == issuer demandé" de Spring Security échoue
 * alors systématiquement au démarrage (`IllegalStateException`).
 *
 * <p>Ce bean construit les registrations directement à partir des points de terminaison
 * connus (aucun appel de découverte), en cohérence avec le commentaire déjà présent dans
 * {@code application.yml} : « la validation effective est assurée par
 * SecurityConfiguration.jwtDecoder() via le phantom token » — la registration {@code oidc}
 * elle-même n'est pas utilisée pour la décision de sécurité, seule {@code keycloak-admin}
 * (client_credentials) sert réellement, au provisioning Keycloak (UTI-01).</p>
 *
 * <p>Étant défini ici, ce bean fait reculer l'auto-configuration Spring Boot
 * ({@code @ConditionalOnMissingBean}) qui, sinon, tenterait la découverte et crasherait —
 * exactement le mécanisme déjà utilisé par {@code TestSecurityConfiguration} en tests.</p>
 */
@ConditionalOnProperty(name = "gimac.iam.keycloak-admin.actif", havingValue = "true")
@Configuration
public class KeycloakClientRegistrationConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${keycloak.server-url}") String serverUrl,
            @Value("${keycloak.realm}") String realm,
            @Value("${spring.security.oauth2.client.registration.keycloak-admin.client-id}") String adminClientId,
            @Value("${spring.security.oauth2.client.registration.keycloak-admin.client-secret}") String adminClientSecret) {

        String base = serverUrl + "/realms/" + realm;

        ClientRegistration oidc = ClientRegistration.withRegistrationId("oidc")
            .clientId("internal")
            .clientSecret("internal")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .scope("openid", "profile", "email", "offline_access")
            .authorizationUri(base + "/protocol/openid-connect/auth")
            .tokenUri(base + "/protocol/openid-connect/token")
            .jwkSetUri(base + "/protocol/openid-connect/certs")
            .userInfoUri(base + "/protocol/openid-connect/userinfo")
            .userNameAttributeName("preferred_username")
            .issuerUri(base)
            .build();

        ClientRegistration keycloakAdmin = ClientRegistration.withRegistrationId("keycloak-admin")
            .clientId(adminClientId)
            .clientSecret(adminClientSecret)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .tokenUri(base + "/protocol/openid-connect/token")
            .build();

        return new InMemoryClientRegistrationRepository(oidc, keycloakAdmin);
    }
}
