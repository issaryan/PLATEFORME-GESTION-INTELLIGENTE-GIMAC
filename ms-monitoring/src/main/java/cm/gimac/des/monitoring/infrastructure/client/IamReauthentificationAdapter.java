package cm.gimac.des.monitoring.infrastructure.client;

import cm.gimac.des.monitoring.application.port.out.ReauthentificationPort;
import cm.gimac.des.monitoring.domain.exception.MonitoringException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptateur réel de la réauthentification FIDO2 (IAM-07), comme la passerelle (IamVerificateurReauthentification)
 * et MS-HOMOLOGATION : POST /api/v1/interne/reauthentifications/verification
 * {@code { assertion, sujet, finalite }} avec le jeton de service monitoring-service (INTERNAL_IAM_READ) ; réponse
 * {@code { valide }}. MS-IAM consomme le défi : l'assertion n'est jamais rejouable. Refus (4xx) : faux ; MS-IAM
 * injoignable ou en erreur : 503 MON_REAUTH_INDISPONIBLE.
 */
@Component
@ConditionalOnProperty(name = "gimac.adaptateurs.reauthentification", havingValue = "real")
public class IamReauthentificationAdapter implements ReauthentificationPort {

    record Demande(String assertion, String sujet, String finalite) {}

    record Verdict(Boolean valide) {}

    private final RestClient client;

    public IamReauthentificationAdapter(RestClient.Builder builder, ServiceTokenInterceptor jeton, @Value("${gimac.services.iam.url}") String url) {
        this.client = builder.baseUrl(url).requestInterceptor(jeton).build();
    }

    @Override
    public boolean verifier(String assertion, String sujet, String finalite) {
        if (assertion == null || assertion.isBlank() || sujet == null || sujet.isBlank()) {
            return false;
        }
        try {
            Verdict v = client
                .post()
                .uri("/api/v1/interne/reauthentifications/verification")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(new Demande(assertion, sujet, finalite))
                .retrieve()
                .body(Verdict.class);
            return v != null && Boolean.TRUE.equals(v.valide());
        } catch (HttpClientErrorException e) {
            return false;
        } catch (RestClientException | IllegalStateException e) {
            throw MonitoringException.indisponible("MON_REAUTH_INDISPONIBLE", "Réauthentification (MS-IAM) indisponible");
        }
    }
}
