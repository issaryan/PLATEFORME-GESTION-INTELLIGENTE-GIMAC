package cm.gimac.des.homologation.config;

import cm.gimac.des.homologation.infrastructure.client.ClientMtls;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.RestClientClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.web.client.RestClient;

/**
 * Jeton de service par client_credentials (PI-GIMAC-01 §0.7, IAM-16) : les appels internes
 * du service portent le jeton du client Keycloak homologation-service, hors de toute requête utilisateur.
 * Paquet SSL {@code gimac.service-token.ssl-bundle} désigné : la demande de jeton se fait en mTLS, et Keycloak lie le
 * jeton au certificat client (cnf.x5t#S256, RFC 8705), exigé par SBX-LECTURE (CC-SBX-GIMAC-02 §D.8).
 */
@Configuration
public class ServiceTokenConfiguration {

    @Bean
    public OAuth2AuthorizedClientManager serviceTokenClientManager(
        ClientRegistrationRepository registrations,
        OAuth2AuthorizedClientService clients,
        SslBundles paquets,
        @Value("${gimac.service-token.ssl-bundle:}") String paquet
    ) {
        AuthorizedClientServiceOAuth2AuthorizedClientManager manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
            registrations,
            clients
        );
        manager.setAuthorizedClientProvider(
            OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials(c -> ClientMtls.fabrique(paquets, paquet).ifPresent(f -> c.accessTokenResponseClient(clientJeton(f))))
                .build()
        );
        return manager;
    }

    /** Client de jeton du fournisseur d'identité, mêmes convertisseurs et erreurs que celui de Spring Security, en mTLS. */
    static RestClientClientCredentialsTokenResponseClient clientJeton(ClientHttpRequestFactory fabrique) {
        RestClientClientCredentialsTokenResponseClient client = new RestClientClientCredentialsTokenResponseClient();
        client.setRestClient(
            RestClient.builder()
                .requestFactory(fabrique)
                .configureMessageConverters(m -> m.addCustomConverter(new FormHttpMessageConverter()).addCustomConverter(new OAuth2AccessTokenResponseHttpMessageConverter()))
                .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler())
                .build()
        );
        return client;
    }
}
