package cm.gimac.des.homologation.infrastructure.client;

import cm.gimac.des.homologation.application.port.out.DocumentRegistryPort;
import cm.gimac.des.homologation.domain.HomologationException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptateur réel : interface interne de MS-DOCUMENT, jeton de service ({@code INTERNAL_DOCUMENT_READ}
 * pour l'empreinte, {@code INTERNAL_DOCUMENT_DEPOSIT} pour le dépôt pour le compte d'un utilisateur).
 * Pièce inconnue : INCONNUE ; service indisponible : 503, sans valeur par défaut.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.document-deposit", havingValue = "real")
public class DocumentRestAdapter implements DocumentRegistryPort {

    record Empreinte(String sha256) {}

    record Depose(UUID id, String sha256, String nomFichier) {}

    record Metadonnees(UUID id, String nomFichier, String typeMime, String sha256) {}

    private final RestClient client;

    public DocumentRestAdapter(RestClient.Builder builder, ServiceTokenInterceptor jeton, @Value("${gimac.services.document.url}") String url) {
        this.client = builder.baseUrl(url).requestInterceptor(jeton).build();
    }

    /**
     * Pièce conservée de même empreinte (US-EXI-14) : {@code GET /api/v1/interne/documents/empreintes/{sha256}}
     * (INTERNAL_DOCUMENT_READ) ; 404 : aucune ; registre injoignable : 503, jamais un dépôt en double supposé.
     */
    @Override
    public java.util.Optional<UUID> parEmpreinte(String sha256) {
        try {
            Metadonnees m = client.get().uri("/api/v1/interne/documents/empreintes/{sha256}", sha256).retrieve().body(Metadonnees.class);
            return java.util.Optional.ofNullable(m).map(Metadonnees::id);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return java.util.Optional.empty();
            }
            throw indisponible();
        } catch (RestClientException ex) {
            throw indisponible();
        }
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
            throw indisponible();
        } catch (RestClientException ex) {
            throw indisponible();
        }
    }

    /** Type MIME inscrit par MS-DOCUMENT ({@code GET /api/v1/interne/documents/{id}}, INTERNAL_DOCUMENT_READ). */
    @Override
    public String typeMime(UUID documentId) {
        try {
            Metadonnees m = client.get().uri("/api/v1/interne/documents/{id}", documentId).retrieve().body(Metadonnees.class);
            return m == null ? null : m.typeMime();
        } catch (HttpClientErrorException ex) {
            return null;
        } catch (RestClientException ex) {
            throw indisponible();
        }
    }

    /** Contenu de la pièce et son nom, lus chez MS-DOCUMENT (D-109) ; pièce inconnue : vide. */
    @Override
    public java.util.Optional<Contenu> lire(UUID documentId) {
        try {
            Metadonnees m = client.get().uri("/api/v1/interne/documents/{id}", documentId).retrieve().body(Metadonnees.class);
            byte[] octets = client.get().uri("/api/v1/interne/documents/{id}/contenu", documentId).retrieve().body(byte[].class);
            if (m == null || octets == null) {
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(new Contenu(octets, m.nomFichier(), m.typeMime()));
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return java.util.Optional.empty();
            }
            throw indisponible();
        } catch (RestClientException ex) {
            throw indisponible();
        }
    }

    @Override
    public Depot deposer(byte[] contenu, String nomFichier, String typeMime, String finalite, String reference, UUID deposantId) {
        var corps = new LinkedMultiValueMap<String, Object>();
        corps.add(
            "fichier",
            new ByteArrayResource(contenu) {
                @Override
                public String getFilename() {
                    return nomFichier;
                }
            }
        );
        corps.add("finalite", finalite);
        corps.add("reference", reference);
        corps.add("deposantId", deposantId == null ? "service-account-homologation-service" : deposantId.toString());
        try {
            Depose d = client.post().uri("/api/v1/interne/documents").contentType(MediaType.MULTIPART_FORM_DATA).body(corps).retrieve().body(Depose.class);
            if (d == null || d.id() == null) {
                throw indisponible();
            }
            return new Depot(d.id(), d.sha256(), d.nomFichier() == null ? nomFichier : d.nomFichier());
        } catch (HttpClientErrorException ex) {
            throw HomologationException.unprocessable("HOM_DOCUMENT_REFUSED", "La pièce a été refusée par le registre documentaire.");
        } catch (RestClientException ex) {
            throw indisponible();
        }
    }

    private static HomologationException indisponible() {
        return HomologationException.unavailable("HOM_DOCUMENT_UNAVAILABLE", "Registre documentaire indisponible");
    }
}
