package cm.gimac.des.iam.config;

import cm.gimac.des.iam.adapter.outbound.keycloak.KeycloakProperties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

/**
 * Fournit le bean Keycloak Admin Client (client_credentials).
 *
 * Dual-Write anti split-brain : ce bean est utilisé pour créer/modifier les
 * utilisateurs dans Keycloak AVANT le flush JPA. Toute défaillance IdP (circuit
 * ouvert Resilience4j) déclenche un rollback SQL propre (MET-27/MET-28).
 *
 * Le secret est injecté depuis Vault via la variable d'environnement
 * VAULT_KEYCLOAK_SECRET (cf. INF-08). Jamais en clair dans les sources.
 */
@ConditionalOnProperty(name = "gimac.iam.keycloak-admin.actif", havingValue = "true")
@Configuration
@EnableConfigurationProperties(KeycloakProperties.class)
public class KeycloakConfig {

    @Bean
    public Keycloak keycloak(
            KeycloakProperties props,
            @SuppressWarnings("unused") OAuth2AuthorizedClientManager clientManager,
            @Value("${spring.security.oauth2.client.registration.keycloak-admin.client-secret}")
            String secret) {

        return KeycloakBuilder.builder()
            .serverUrl(props.getServerUrl())
            .realm(props.getRealm())
            .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
            .clientId(props.getAdminClientId())
            .clientSecret(secret)
            .build();
    }
}
