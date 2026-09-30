package cm.gimac.des.certification.infrastructure.client;

import cm.gimac.des.certification.application.port.out.ReauthentificationPort;
import cm.gimac.des.certification.domain.CertificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptateur réel : MS-IAM vérifie l'assertion FIDO2 (IAM-07) et la consomme, ce qui interdit son rejeu
 * ({@code POST /api/v1/interne/reauthentifications/verification}, INTERNAL_IAM_READ). Refus (4xx) : faux ;
 * MS-IAM indisponible : 503, sans valeur par défaut.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.reauthentication", havingValue = "real")
public class IamReauthentificationAdapter implements ReauthentificationPort {

    record Demande(String assertion, String sujet, String finalite) {}

    record Verdict(boolean valide) {}

    private final RestClient client;

    public IamReauthentificationAdapter(RestClient.Builder builder, ServiceTokenInterceptor jeton, @Value("${gimac.services.iam.url}") String url) {
        this.client = builder.baseUrl(url).requestInterceptor(jeton).build();
    }

    @Override
    public boolean verifier(String assertion, String sujet, String finalite) {
        if (assertion == null || assertion.isBlank()) {
            return false;
        }
        try {
            Verdict v = client
                .post()
                .uri("/api/v1/interne/reauthentifications/verification")
                .body(new Demande(assertion, sujet, finalite))
                .retrieve()
                .body(Verdict.class);
            return v != null && v.valide();
        } catch (HttpClientErrorException e) {
            return false;
        } catch (RestClientException | IllegalStateException e) {
            throw CertificationException.indisponible("CER_IAM_UNAVAILABLE", "Service de réauthentification indisponible");
        }
    }
}
