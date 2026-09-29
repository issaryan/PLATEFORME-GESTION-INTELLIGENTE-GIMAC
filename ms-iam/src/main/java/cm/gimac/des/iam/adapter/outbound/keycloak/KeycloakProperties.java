package cm.gimac.des.iam.adapter.outbound.keycloak;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriétés de connexion au serveur Keycloak.
 * Injectées depuis application.yml (section keycloak:).
 * Activées par @EnableConfigurationProperties(KeycloakProperties.class).
 */
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

    private String serverUrl;
    private String realm;
    private String adminClientId;
    private String clientId;

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getAdminClientId() {
        return adminClientId;
    }

    public void setAdminClientId(String adminClientId) {
        this.adminClientId = adminClientId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}
