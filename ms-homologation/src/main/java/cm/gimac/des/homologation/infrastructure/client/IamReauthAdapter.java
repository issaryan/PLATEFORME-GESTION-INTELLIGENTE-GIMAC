package cm.gimac.des.homologation.infrastructure.client;

import cm.gimac.des.homologation.application.port.out.StrongReauthPort;
import cm.gimac.des.homologation.domain.HomologationException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptateur réel : MS-IAM vérifie l'assertion FIDO2 (IAM-07) et la consomme, ce qui interdit son
 * rejeu. Refus (4xx) : faux ; MS-IAM indisponible : 503, sans valeur par défaut.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.reauthentication", havingValue = "real")
public class IamReauthAdapter implements StrongReauthPort {

    record Demande(String assertion, UUID sujet, String finalite) {}

    record Verdict(boolean valide) {}

    private final RestClient client;

    public IamReauthAdapter(RestClient.Builder builder, ServiceTokenInterceptor jeton, @Value("${gimac.services.iam.url}") String url) {
        this.client = builder.baseUrl(url).requestInterceptor(jeton).build();
    }

    @Override
    public boolean verifier(String assertion, UUID sujet, String finalite) {
        if (assertion == null || assertion.isBlank()) {
            return false;
        }
        try {
            Verdict v = client.post().uri("/api/v1/interne/reauthentifications/verification").body(new Demande(assertion, sujet, finalite)).retrieve().body(Verdict.class);
            return v != null && v.valide();
        } catch (HttpClientErrorException ex) {
            return false;
        } catch (RestClientException ex) {
            throw HomologationException.unavailable("HOM_IAM_UNAVAILABLE", "Service de réauthentification indisponible");
        }
    }
}
