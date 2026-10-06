package cm.gimac.des.gateway.config;

import cm.gimac.des.gateway.domain.supervision.ServiceCible;
import cm.gimac.des.gateway.infrastructure.supervision.ActuatorWebClientAdapter;
import cm.gimac.des.gateway.infrastructure.supervision.AlertmanagerWebClientAdapter;
import cm.gimac.des.gateway.infrastructure.supervision.ClientsSupervision;
import cm.gimac.des.gateway.infrastructure.supervision.JetonServiceSupervision;
import cm.gimac.des.gateway.infrastructure.supervision.PrometheusWebClientAdapter;
import cm.gimac.des.gateway.infrastructure.supervision.RoutesPasserelleAdapter;
import cm.gimac.des.gateway.service.supervision.SupervisionPlateformeService;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;

/**
 * Assemblage du module de supervision de la plateforme (hexagonal) : adaptateurs des ports
 * sortants (actionneurs des services, Prometheus, Alertmanager, routes) et service d'application.
 */
@Configuration
@EnableConfigurationProperties(SupervisionProperties.class)
public class SupervisionConfiguration {

    public static final String PASSERELLE = "gateway";

    @Bean
    public SupervisionPlateformeService supervisionPlateformeService(
        SupervisionProperties proprietes,
        RouteLocator routeLocator,
        ObjectProvider<ReactiveClientRegistrationRepository> enregistrements,
        ObjectProvider<ReactiveOAuth2AuthorizedClientService> clientsAutorises
    ) {
        SupervisionProperties.Supervision s = proprietes.getSupervision();
        JetonServiceSupervision jeton = new JetonServiceSupervision(gestionnaire(enregistrements, clientsAutorises));
        return new SupervisionPlateformeService(
            parametres(proprietes),
            new ActuatorWebClientAdapter(ClientsSupervision.webClient(s.getDelai(), null), jeton),
            new PrometheusWebClientAdapter(ClientsSupervision.webClient(s.getDelai(), s.getPrometheusUrl()), s.getProjetConteneurs()),
            new AlertmanagerWebClientAdapter(ClientsSupervision.webClient(s.getDelai(), s.getAlertmanagerUrl())),
            new RoutesPasserelleAdapter(routeLocator),
            Clock.systemUTC()
        );
    }

    /** Services supervisés : la passerelle d'abord, puis gimac.services dans l'ordre de la configuration. */
    static SupervisionPlateformeService.Parametres parametres(SupervisionProperties proprietes) {
        SupervisionProperties.Supervision s = proprietes.getSupervision();
        List<ServiceCible> services = new ArrayList<>();
        if (s.getPasserelleUrl() != null && !s.getPasserelleUrl().isBlank()) {
            services.add(new ServiceCible(PASSERELLE, s.getPasserelleUrl()));
        }
        proprietes
            .getServices()
            .forEach((nom, url) -> {
                if (!PASSERELLE.equals(nom) && url != null && !url.isBlank()) {
                    services.add(new ServiceCible(nom, url));
                }
            });
        return new SupervisionPlateformeService.Parametres(services, s.getDelai(), s.getIntervalleFlux(), s.getKeycloakSanteUrl());
    }

    /** Gestionnaire client_credentials utilisable hors requête ; null sans client OAuth2 configuré. */
    static AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager gestionnaire(
        ObjectProvider<ReactiveClientRegistrationRepository> enregistrements,
        ObjectProvider<ReactiveOAuth2AuthorizedClientService> clientsAutorises
    ) {
        ReactiveClientRegistrationRepository depot = enregistrements.getIfAvailable();
        ReactiveOAuth2AuthorizedClientService service = clientsAutorises.getIfAvailable();
        if (depot == null || service == null) {
            return null;
        }
        var gestionnaire = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(depot, service);
        gestionnaire.setAuthorizedClientProvider(ReactiveOAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build());
        return gestionnaire;
    }
}
