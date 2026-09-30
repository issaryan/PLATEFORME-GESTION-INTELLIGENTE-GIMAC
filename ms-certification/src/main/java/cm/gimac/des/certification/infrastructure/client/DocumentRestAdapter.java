package cm.gimac.des.certification.infrastructure.client;

import cm.gimac.des.certification.application.port.out.DocumentPort;
import cm.gimac.des.certification.domain.CertificationException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptateur réel de MS-DOCUMENT (jeton de service) : empreinte d'un document
 * ({@code GET /api/v1/interne/documents/{id}/empreinte}, INTERNAL_DOCUMENT_READ) et dépôt pour le compte d'un
 * utilisateur ({@code POST /api/v1/interne/documents}, INTERNAL_DOCUMENT_DEPOSIT).
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.document-deposit", havingValue = "real")
public class DocumentRestAdapter implements DocumentPort {

    record Empreinte(String sha256) {}

    record Depose(UUID id, String sha256) {}

    private final RestClient client;

    public DocumentRestAdapter(RestClient.Builder builder, ServiceTokenInterceptor jeton, @Value("${gimac.services.document.url}") String url) {
        this.client = builder.baseUrl(url).requestInterceptor(jeton).build();
    }

    @Override
    public boolean empreinteConforme(UUID documentId, String sha256) {
        try {
            Empreinte e = client.get().uri("/api/v1/interne/documents/{id}/empreinte", documentId).retrieve().body(Empreinte.class);
            return e != null && e.sha256() != null && e.sha256().equalsIgnoreCase(sha256);
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (RestClientException | IllegalStateException e) {
            throw indisponible();
        }
    }

    @Override
    public DocumentDepose deposer(Fichier fichier, String finalite, String reference, String deposantId) {
        MultiValueMap<String, Object> corps = new LinkedMultiValueMap<>();
        corps.add(
            "fichier",
            new ByteArrayResource(fichier.octets()) {
                @Override
                public String getFilename() {
                    return fichier.nom();
                }
            }
        );
        corps.add("finalite", finalite);
        corps.add("reference", reference);
        corps.add("deposantId", deposantId);
        try {
            Depose d = client.post().uri("/api/v1/interne/documents").contentType(MediaType.MULTIPART_FORM_DATA).body(corps).retrieve().body(Depose.class);
            if (d == null || d.id() == null) {
                throw indisponible();
            }
            return new DocumentDepose(d.id().toString(), d.sha256());
        } catch (HttpClientErrorException e) {
            throw CertificationException.irrecevable("CER_ATTESTATION_FORMAT", "La pièce a été refusée par le service documentaire.");
        } catch (RestClientException | IllegalStateException e) {
            throw indisponible();
        }
    }

    private static CertificationException indisponible() {
        return CertificationException.indisponible("CER_DOCUMENT_UNAVAILABLE", "Service documentaire indisponible");
    }
}
