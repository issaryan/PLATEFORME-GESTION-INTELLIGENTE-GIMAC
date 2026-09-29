package cm.gimac.des.participant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * Jeton de service par client_credentials (PI-GIMAC-01 §0.7, IAM-16) : les appels internes
 * du service portent le jeton du client Keycloak participant-service, hors de toute requête utilisateur.
 */
@Configuration
public class ServiceTokenConfiguration {

    @Bean
    public OAuth2AuthorizedClientManager serviceTokenClientManager(
        ClientRegistrationRepository registrations,
        OAuth2AuthorizedClientService clients
    ) {
        AuthorizedClientServiceOAuth2AuthorizedClientManager manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
            registrations,
            clients
        );
        manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build());
        return manager;
    }
}
