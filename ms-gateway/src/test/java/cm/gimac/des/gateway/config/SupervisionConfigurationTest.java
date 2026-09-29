package cm.gimac.des.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import cm.gimac.des.gateway.domain.supervision.ServiceCible;
import cm.gimac.des.gateway.service.supervision.SupervisionPlateformeService;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import reactor.core.publisher.Flux;

/** Assemblage du module de supervision : services supervisés, délais, gestionnaire du jeton de service. */
class SupervisionConfigurationTest {

    @SuppressWarnings("unchecked")
    static <T> ObjectProvider<T> fournisseur(T valeur) {
        ObjectProvider<T> p = mock(ObjectProvider.class);
        org.mockito.Mockito.when(p.getIfAvailable()).thenReturn(valeur);
        return p;
    }

    static SupervisionProperties proprietes() {
        SupervisionProperties p = new SupervisionProperties();
        Map<String, String> services = new LinkedHashMap<>();
        services.put("iam", "http://iam:8081");
        services.put("gateway", "http://ignore:8080");
        services.put("audit", " ");
        services.put("participant", "http://participant:8085");
        p.setServices(services);
        return p;
    }

    @Test
    @DisplayName("Services supervisés : la passerelle d'abord, puis gimac.services dans l'ordre, adresses vides ignorées")
    void parametres() {
        SupervisionProperties p = proprietes();
        SupervisionProperties.Supervision s = p.getSupervision();
        s.setPasserelleUrl("http://localhost:8080");
        s.setDelai(Duration.ofMillis(500));
        s.setIntervalleFlux(Duration.ofSeconds(10));
        s.setPrometheusUrl("http://prometheus:9090");
        s.setAlertmanagerUrl("http://alertmanager:9093");
        s.setKeycloakSanteUrl("http://keycloak:9990/health/ready");
        SupervisionPlateformeService.Parametres parametres = SupervisionConfiguration.parametres(p);
        assertThat(parametres.services()).containsExactly(
            new ServiceCible("gateway", "http://localhost:8080"),
            new ServiceCible("iam", "http://iam:8081"),
            new ServiceCible("participant", "http://participant:8085")
        );
        assertThat(parametres.delai()).isEqualTo(Duration.ofMillis(500));
        assertThat(parametres.intervalleFlux()).isEqualTo(Duration.ofSeconds(10));
        assertThat(parametres.keycloakSanteUrl()).isEqualTo("http://keycloak:9990/health/ready");
        assertThat(s.getPrometheusUrl()).isEqualTo("http://prometheus:9090");
        assertThat(s.getAlertmanagerUrl()).isEqualTo("http://alertmanager:9093");

        s.setPasserelleUrl("");
        assertThat(SupervisionConfiguration.parametres(p).services()).extracting(ServiceCible::nom).containsExactly("iam", "participant");
        s.setPasserelleUrl(null);
        assertThat(SupervisionConfiguration.parametres(p).services()).hasSize(2);
    }

    @Test
    @DisplayName("Valeurs par défaut : délai 2 s, flux 5 s, Prometheus, Alertmanager et Keycloak locaux")
    void defauts() {
        SupervisionProperties.Supervision s = new SupervisionProperties().getSupervision();
        assertThat(s.getDelai()).isEqualTo(Duration.ofSeconds(2));
        assertThat(s.getIntervalleFlux()).isEqualTo(Duration.ofSeconds(5));
        assertThat(s.getPasserelleUrl()).isEqualTo("http://localhost:8080");
        assertThat(s.getPrometheusUrl()).isEqualTo("http://localhost:9090");
        assertThat(s.getAlertmanagerUrl()).isEqualTo("http://localhost:9093");
        assertThat(s.getKeycloakSanteUrl()).isEqualTo("http://localhost:9990/health/ready");
    }

    @Test
    @DisplayName("Gestionnaire client_credentials seulement si les enregistrements OAuth2 existent ; service assemblé")
    void assemblage() {
        ReactiveClientRegistrationRepository depot = mock(ReactiveClientRegistrationRepository.class);
        ReactiveOAuth2AuthorizedClientService clients = mock(ReactiveOAuth2AuthorizedClientService.class);
        assertThat(SupervisionConfiguration.gestionnaire(fournisseur(depot), fournisseur(clients))).isNotNull();
        assertThat(SupervisionConfiguration.gestionnaire(fournisseur(null), fournisseur(clients))).isNull();
        assertThat(SupervisionConfiguration.gestionnaire(fournisseur(depot), fournisseur(null))).isNull();

        RouteLocator routes = Flux::empty;
        SupervisionPlateformeService service = new SupervisionConfiguration().supervisionPlateformeService(
            proprietes(),
            routes,
            fournisseur(depot),
            fournisseur(clients)
        );
        assertThat(service.services()).extracting(ServiceCible::nom).containsExactly("gateway", "iam", "participant");
    }
}
