package cm.gimac.des.gateway.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Propriétés des routes de la Sandbox GIMAC (gimac.sandbox, D-120, US-EXI-07). */
@ConfigurationProperties(prefix = "gimac.sandbox")
public class SandboxProperties {

    /** Requêtes admises par minute pour un titulaire (institution effective ou compte du GIMAC) et une route, par couche. */
    private Map<String, Integer> quotas = new LinkedHashMap<>(Map.of("portail", 120, "console", 300, "des", 120));

    /** Projection des périmètres de SBX-ESPACE. */
    private final Perimetres perimetres = new Perimetres();

    public Map<String, Integer> getQuotas() {
        return quotas;
    }

    public void setQuotas(Map<String, Integer> quotas) {
        this.quotas = quotas;
    }

    public Perimetres getPerimetres() {
        return perimetres;
    }

    public static class Perimetres {

        /** Consommation de sbx-espace.scope-changed.v1 active. */
        private boolean actif;

        private String sujet = "sbx-espace.scope-changed.v1";

        private String bootstrapServers = "localhost:9092";

        /** Propriétés supplémentaires du consommateur (sécurité du courtier…). */
        private Map<String, String> kafka = new LinkedHashMap<>();

        public boolean isActif() {
            return actif;
        }

        public void setActif(boolean actif) {
            this.actif = actif;
        }

        public String getSujet() {
            return sujet;
        }

        public void setSujet(String sujet) {
            this.sujet = sujet;
        }

        public String getBootstrapServers() {
            return bootstrapServers;
        }

        public void setBootstrapServers(String bootstrapServers) {
            this.bootstrapServers = bootstrapServers;
        }

        public Map<String, String> getKafka() {
            return kafka;
        }

        public void setKafka(Map<String, String> kafka) {
            this.kafka = kafka;
        }
    }
}
