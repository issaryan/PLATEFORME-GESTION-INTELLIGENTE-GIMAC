package cm.gimac.des.audit.adapter.outbound.securite;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Fournisseur de jeton de service (client_credentials) — ADM-17.
 *
 * Le service appelé valide un JWT Keycloak RS256 porteur du rôle de service de
 * l'appelant (comptes de service, IAM-16). Le mTLS prouve l'identité du canal ; le JWT prouve
 * l'autorisation applicative. Ce composant obtient et met en cache ce jeton, le rafraîchissant
 * avant expiration. Inactif tant que {@code gimac.securite.jeton-service.actif} n'est pas vrai.
 */
@ConditionalOnProperty(name = "gimac.securite.jeton-service.actif", havingValue = "true")
@Component
public class ServiceTokenProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceTokenProvider.class);
    private static final Duration MARGE_EXPIRATION = Duration.ofSeconds(30);

    private final RestClient tokenClient;
    private final String clientId;
    private final String clientSecret;

    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    public ServiceTokenProvider(
        @Value("${keycloak.server-url:http://keycloak:9080}") String serverUrl,
        @Value("${keycloak.realm:jhipster}") String realm,
        @Value("${keycloak.admin-client-id:internal}") String clientId,
        @Value("${keycloak.admin-client-secret:}") String clientSecret
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        String tokenUri = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";
        this.tokenClient = RestClient.builder().baseUrl(tokenUri).build();
    }

    /** Renvoie un jeton d'accès valide (depuis le cache ou fraîchement obtenu), ou {@code null}. */
    @SuppressWarnings("unchecked")
    public synchronized String obtenirJeton() {
        if (cachedToken != null && Instant.now().isBefore(expiresAt)) {
            return cachedToken;
        }
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credentials");
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);

            Map<String, Object> reponse = tokenClient
                .post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

            if (reponse == null || reponse.get("access_token") == null) {
                LOG.warn("Jeton de service indisponible — réponse Keycloak vide");
                return null;
            }
            cachedToken = reponse.get("access_token").toString();
            long ttl = reponse.get("expires_in") != null
                ? Long.parseLong(reponse.get("expires_in").toString()) : 60L;
            expiresAt = Instant.now().plusSeconds(ttl).minus(MARGE_EXPIRATION);
            return cachedToken;
        } catch (RuntimeException e) {
            LOG.warn("Échec obtention jeton de service (client_credentials {}): {}", clientId, e.getMessage());
            return null;
        }
    }
}
