package cm.gimac.des.gateway.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Connexion sur la plateforme (gimac.connexion, D-65). Les clients Keycloak sont des
 * enregistrements OAuth2 (spring.security.oauth2.client.registration) : « connexion »
 * (gimac-connexion, secret GIMAC_SECRET_CONNEXION), « oidc » (web_app) et « gateway-service »
 * (jeton de service vers MS-IAM).
 */
@ConfigurationProperties(prefix = "gimac.connexion")
public class ConnexionProperties {

    /** Adresse de MS-IAM (routes internes /api/v1/interne/**). */
    private String iamUrl = "http://localhost:8081";

    /** Délai maximal d'un appel à Keycloak ou à MS-IAM. */
    private Duration delai = Duration.ofSeconds(5);

    /** Tentatives admises par minute, par adresse IP et par identifiant (429 au-delà). */
    private int tentativesParMinute = 5;

    public String getIamUrl() {
        return iamUrl;
    }

    public void setIamUrl(String iamUrl) {
        this.iamUrl = iamUrl;
    }

    public Duration getDelai() {
        return delai;
    }

    public void setDelai(Duration delai) {
        this.delai = delai;
    }

    public int getTentativesParMinute() {
        return tentativesParMinute;
    }

    public void setTentativesParMinute(int tentativesParMinute) {
        this.tentativesParMinute = tentativesParMinute;
    }
}
