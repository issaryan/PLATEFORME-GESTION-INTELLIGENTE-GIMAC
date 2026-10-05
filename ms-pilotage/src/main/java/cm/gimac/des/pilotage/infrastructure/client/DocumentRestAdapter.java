package cm.gimac.des.pilotage.infrastructure.client;

import cm.gimac.des.pilotage.application.port.out.DocumentPort;
import cm.gimac.des.pilotage.domain.exception.PilotageException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptateur réel des documents des rapports : interface interne de MS-DOCUMENT (jeton de service du
 * client pilotage-service, INTERNAL_DOCUMENT_READ). Métadonnées d'un ticket :
 * GET /api/v1/interne/documents/{id} ; contenu : GET /api/v1/interne/documents/{id}/contenu, flux
 * transmis sans copie intégrale en mémoire (docs/lot1/depots-fragmentes.md §4).
 */
@Component
@ConditionalOnProperty(name = "gimac.adaptateurs.documents", havingValue = "real")
public class DocumentRestAdapter implements DocumentPort {

    /** Réponse de GET /api/v1/interne/documents/{id}, champs utiles seulement. */
    record MetadonneesIam(UUID id, String nomFichier, Long taille, String typeMime, String sha256, String finalite, String reference, String deposant) {}

    private final RestClient client;

    public DocumentRestAdapter(RestClient.Builder builder, ServiceTokenInterceptor jeton, @Value("${gimac.services.document.url}") String url) {
        this.client = builder.baseUrl(url).requestInterceptor(jeton).build();
    }

    @Override
    public Optional<Metadonnees> metadonnees(UUID documentId) {
        MetadonneesIam m;
        try {
            m = client
                .get()
                .uri("/api/v1/interne/documents/{id}", documentId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange((requete, reponse) -> {
                    if (reponse.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                        return null;
                    }
                    if (!reponse.getStatusCode().is2xxSuccessful()) {
                        throw indisponible();
                    }
                    return reponse.bodyTo(MetadonneesIam.class);
                });
        } catch (RestClientException | IllegalStateException e) {
            throw indisponible();
        }
        if (m == null) {
            return Optional.empty();
        }
        if (m.id() == null || m.taille() == null) {
            // Réponse incomplète : on ne suppose aucune valeur.
            throw indisponible();
        }
        return Optional.of(new Metadonnees(m.id(), m.nomFichier(), m.taille(), m.typeMime(), m.sha256(), m.finalite(), m.reference(), m.deposant()));
    }

    @Override
    public Contenu lire(UUID documentId) {
        try {
            return client
                .get()
                .uri("/api/v1/interne/documents/{id}/contenu", documentId)
                .exchange(
                    (requete, reponse) -> {
                        if (reponse.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                            reponse.close();
                            throw PilotageException.introuvable("PIL_DOCUMENT_ABSENT", documentId);
                        }
                        if (!reponse.getStatusCode().is2xxSuccessful()) {
                            reponse.close();
                            throw indisponible();
                        }
                        return new Contenu(reponse.getBody(), reponse.getHeaders().getContentLength());
                    },
                    false
                );
        } catch (RestClientException | IllegalStateException e) {
            throw indisponible();
        }
    }

    private static PilotageException indisponible() {
        return PilotageException.indisponible("PIL_DOCUMENT_INDISPONIBLE", "Dépôt documentaire (MS-DOCUMENT) indisponible");
    }
}
