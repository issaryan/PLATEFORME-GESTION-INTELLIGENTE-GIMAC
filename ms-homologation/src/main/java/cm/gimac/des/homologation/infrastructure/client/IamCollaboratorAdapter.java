package cm.gimac.des.homologation.infrastructure.client;

import cm.gimac.des.homologation.application.port.out.CollaboratorDirectoryPort;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.cadrage.CollaboratorView;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Adaptateur réel : annuaire interne de MS-IAM (/api/v1/interne/collaborateurs), jeton de service. */
@Component
@ConditionalOnProperty(name = "gimac.adapters.collaborator-reader", havingValue = "real")
public class IamCollaboratorAdapter implements CollaboratorDirectoryPort {

    record Collaborateur(String id, String nomAffiche, Set<String> profils, String institutionId, boolean actif) {
        CollaboratorView vue() {
            return new CollaboratorView(UUID.fromString(id), nomAffiche, profils, institutionId, actif);
        }
    }

    private final RestClient client;

    public IamCollaboratorAdapter(RestClient.Builder builder, ServiceTokenInterceptor jeton, @Value("${gimac.services.iam.url}") String url) {
        this.client = builder.baseUrl(url).requestInterceptor(jeton).build();
    }

    @Override
    public Optional<CollaboratorView> findCollaborator(UUID userId) {
        try {
            return Optional.ofNullable(client.get().uri("/api/v1/interne/collaborateurs/{id}", userId).retrieve().body(Collaborateur.class)).map(Collaborateur::vue);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException | IllegalStateException e) {
            throw indisponible();
        }
    }

    @Override
    public List<CollaboratorView> parProfil(String profil) {
        try {
            Collaborateur[] l = client.get().uri(u -> u.path("/api/v1/interne/collaborateurs").queryParam("profil", profil).build()).retrieve().body(Collaborateur[].class);
            return l == null ? List.of() : Arrays.stream(l).map(Collaborateur::vue).toList();
        } catch (RestClientException | IllegalStateException e) {
            throw indisponible();
        }
    }

    /**
     * Porteurs d'une permission (D-94 §3), {@code GET /api/v1/interne/collaborateurs?permission=} (MS-IAM,
     * docs/lot1/ecrans-par-configuration.md §6.6) : tableau de collaborateurs ; réponse vide : aucun porteur. Toute
     * autre issue (MS-IAM injoignable, 5xx, refus 4xx du jeton de service, réponse qui n'est pas un tableau) : 503
     * HOM_IAM_UNAVAILABLE, sans repli.
     */
    @Override
    public List<CollaboratorView> parPermission(String permission) {
        try {
            Collaborateur[] l = client.get().uri(u -> u.path("/api/v1/interne/collaborateurs").queryParam("permission", permission).build()).retrieve().body(Collaborateur[].class);
            return l == null ? List.of() : Arrays.stream(l).map(Collaborateur::vue).toList();
        } catch (RestClientException | IllegalStateException e) {
            throw indisponible();
        }
    }

    private static HomologationException indisponible() {
        return HomologationException.unavailable("HOM_IAM_UNAVAILABLE", "Annuaire des collaborateurs indisponible");
    }
}
