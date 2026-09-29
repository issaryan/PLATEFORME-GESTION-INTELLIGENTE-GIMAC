package cm.gimac.des.participant.infrastructure.client;

import cm.gimac.des.participant.application.port.out.DocumentRegistry;
import cm.gimac.des.participant.domain.ParticipantException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptateur réel du registre documentaire : interface interne de MS-DOCUMENT, appelée avec le
 * jeton de service. Pièce inconnue : INCONNUE ; service indisponible : 503, sans valeur par défaut.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.document-registry", havingValue = "real")
public class HttpDocumentRegistry implements DocumentRegistry {

    /** Réponse de MS-DOCUMENT : seule l'empreinte est lue. */
    record Empreinte(String sha256) {}

    private final RestClient client;

    public HttpDocumentRegistry(RestClient.Builder builder, ServiceTokenInterceptor jeton, @Value("${gimac.services.document.url}") String url) {
        this.client = builder.baseUrl(url).requestInterceptor(jeton).build();
    }

    @Override
    public Verification verifier(UUID documentId, String sha256) {
        try {
            Empreinte e = client.get().uri("/api/v1/interne/documents/{id}/empreinte", documentId).retrieve().body(Empreinte.class);
            if (e == null || e.sha256() == null) {
                return Verification.INCONNUE;
            }
            return e.sha256().equalsIgnoreCase(sha256) ? Verification.CONFORME : Verification.DIVERGENTE;
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Verification.INCONNUE;
            }
            throw ParticipantException.unavailable("PAR_DOCUMENT_UNAVAILABLE", "Registre documentaire indisponible");
        } catch (RestClientException ex) {
            throw ParticipantException.unavailable("PAR_DOCUMENT_UNAVAILABLE", "Registre documentaire indisponible");
        }
    }
}
