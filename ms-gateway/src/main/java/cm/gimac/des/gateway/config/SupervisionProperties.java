package cm.gimac.des.gateway.config;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriétés de la supervision de la plateforme (D-61) : adresses des services
 * (gimac.services.*, déjà utilisées par les routes) et bloc gimac.supervision.
 */
@ConfigurationProperties(prefix = "gimac")
public class SupervisionProperties {

    /** Adresse de base de chaque microservice, par nom (gimac.services.iam…). */
    private Map<String, String> services = new LinkedHashMap<>();

    private final Supervision supervision = new Supervision();

    public Map<String, String> getServices() {
        return services;
    }

    public void setServices(Map<String, String> services) {
        this.services = new LinkedHashMap<>(services);
    }

    public Supervision getSupervision() {
        return supervision;
    }

    /** Bloc gimac.supervision. */
    public static class Supervision {

        /** Adresse de la passerelle elle-même, supervisée comme les autres services (vide : non supervisée). */
        private String passerelleUrl = "http://localhost:8080";

        /** Délai de chaque appel (santé, info, métriques, Prometheus…) : au-delà, le service est injoignable. */
        private Duration delai = Duration.ofSeconds(2);

        /** Période du flux temps réel. */
        private Duration intervalleFlux = Duration.ofSeconds(5);

        /** API HTTP de Prometheus. */
        private String prometheusUrl = "http://localhost:9090";

        /** API HTTP d'Alertmanager. */
        private String alertmanagerUrl = "http://localhost:9093";

        /** Sonde de santé de Keycloak (port de gestion) ; vide : non sondé. */
        private String keycloakSanteUrl = "http://localhost:9990/health/ready";

        public String getPasserelleUrl() {
            return passerelleUrl;
        }

        public void setPasserelleUrl(String passerelleUrl) {
            this.passerelleUrl = passerelleUrl;
        }

        public Duration getDelai() {
            return delai;
        }

        public void setDelai(Duration delai) {
            this.delai = delai;
        }

        public Duration getIntervalleFlux() {
            return intervalleFlux;
        }

        public void setIntervalleFlux(Duration intervalleFlux) {
            this.intervalleFlux = intervalleFlux;
        }

        public String getPrometheusUrl() {
            return prometheusUrl;
        }

        public void setPrometheusUrl(String prometheusUrl) {
            this.prometheusUrl = prometheusUrl;
        }

        public String getAlertmanagerUrl() {
            return alertmanagerUrl;
        }

        public void setAlertmanagerUrl(String alertmanagerUrl) {
            this.alertmanagerUrl = alertmanagerUrl;
        }

        public String getKeycloakSanteUrl() {
            return keycloakSanteUrl;
        }

        public void setKeycloakSanteUrl(String keycloakSanteUrl) {
            this.keycloakSanteUrl = keycloakSanteUrl;
        }
    }
}
