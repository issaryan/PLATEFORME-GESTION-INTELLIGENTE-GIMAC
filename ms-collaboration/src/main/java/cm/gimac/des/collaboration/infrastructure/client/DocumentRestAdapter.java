package cm.gimac.des.collaboration.infrastructure.client;

import cm.gimac.des.collaboration.application.port.out.DocumentStoragePort;
import cm.gimac.des.collaboration.domain.exception.CollaborationException;
import cm.gimac.des.collaboration.domain.messagerie.FichierJoint;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptateur réel du dépôt documentaire : interface interne de MS-DOCUMENT, jeton de service.
 * Dépôt : POST /api/v1/interne/documents (multipart fichier, finalite=MESSAGERIE, reference=message,
 * deposantId ; INTERNAL_DOCUMENT_DEPOSIT). Lecture : GET /api/v1/interne/documents/{id}/contenu
 * (INTERNAL_DOCUMENT_READ), flux transmis sans copie intégrale en mémoire.
 */
@Component
@ConditionalOnProperty(name = "gimac.adaptateurs.documents", havingValue = "real")
public class DocumentRestAdapter implements DocumentStoragePort {

    record Depot(UUID id, String sha256, String nomFichier, Long taille, String typeMime) {}

    private final RestClient client;

    public DocumentRestAdapter(RestClient.Builder builder, ServiceTokenInterceptor jeton, @Value("${gimac.services.document.url}") String url) {
        this.client = builder.baseUrl(url).requestInterceptor(jeton).build();
    }

    @Override
    public DocumentDepose deposer(FichierJoint fichier, UUID messageId, String deposantId) {
        try {
            MultipartBodyBuilder corps = new MultipartBodyBuilder();
            corps.part("fichier", new Contenu(fichier)).contentType(MediaType.parseMediaType(fichier.typeMime())).filename(fichier.nom());
            corps.part("finalite", FINALITE);
            corps.part("reference", messageId.toString());
            corps.part("deposantId", deposantId);
            Depot d = client.post().uri("/api/v1/interne/documents").contentType(MediaType.MULTIPART_FORM_DATA).body(corps.build()).retrieve().body(Depot.class);
            if (d == null || d.id() == null) {
                throw indisponible();
            }
            return new DocumentDepose(
                d.id(),
                d.sha256(),
                d.nomFichier() == null ? fichier.nom() : d.nomFichier(),
                d.taille() == null ? fichier.taille() : d.taille(),
                d.typeMime() == null ? fichier.typeMime() : d.typeMime()
            );
        } catch (HttpClientErrorException e) {
            // Refus du dépôt (par exemple BNF-16 : numéro de carte en clair dans le fichier).
            throw CollaborationException.invalide(
                "COL_ATTACHMENT_REJECTED",
                "Pièce jointe refusée par le dépôt documentaire",
                Map.of("nom", fichier.nom(), "statut", e.getStatusCode().value())
            );
        } catch (RestClientException | IllegalStateException | UncheckedIOException e) {
            throw indisponible();
        }
    }

    @Override
    public ContenuDocument lire(UUID documentId) {
        try {
            return client
                .get()
                .uri("/api/v1/interne/documents/{id}/contenu", documentId)
                .exchange(
                    (requete, reponse) -> {
                        if (reponse.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                            reponse.close();
                            throw CollaborationException.introuvable("COL_ATTACHMENT_NOT_FOUND", documentId);
                        }
                        if (!reponse.getStatusCode().is2xxSuccessful()) {
                            reponse.close();
                            throw indisponible();
                        }
                        return new ContenuDocument(reponse.getBody(), reponse.getHeaders().getContentLength());
                    },
                    false
                );
        } catch (RestClientException | IllegalStateException e) {
            throw indisponible();
        }
    }

    private static CollaborationException indisponible() {
        return CollaborationException.indisponible("COL_DOCUMENT_UNAVAILABLE", "Dépôt documentaire indisponible");
    }

    /** Partie « fichier » lue à la demande ; la longueur est connue sans consommer le flux (nom : en-tête de la partie). */
    private static final class Contenu extends InputStreamResource {

        private final FichierJoint fichier;

        Contenu(FichierJoint fichier) {
            super(ouvrir(fichier));
            this.fichier = fichier;
        }

        private static InputStream ouvrir(FichierJoint f) {
            try {
                return f.source().ouvrir();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public long contentLength() {
            return fichier.taille();
        }
    }
}
