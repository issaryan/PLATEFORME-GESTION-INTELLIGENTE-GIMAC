package cm.gimac.des.collaboration.infrastructure.client;

import cm.gimac.des.collaboration.application.port.out.CollaboratorDirectoryPort;
import cm.gimac.des.collaboration.domain.exception.CollaborationException;
import cm.gimac.des.collaboration.domain.messagerie.Collaborateur;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptateur réel de l'annuaire : interface interne de MS-IAM (/api/v1/interne/collaborateurs,
 * permission INTERNAL_IAM_READ), jeton de service du client collaboration-service.
 */
@Component
@ConditionalOnProperty(name = "gimac.adaptateurs.annuaire", havingValue = "real")
public class IamCollaboratorAdapter implements CollaboratorDirectoryPort {

    record CollaborateurIam(String id, String nomAffiche, Set<String> profils, String institutionId, Boolean actif, String fonction, String avatarVersion) {
        Collaborateur vue() {
            return new Collaborateur(id, nomAffiche, fonction, profils, institutionId, Boolean.TRUE.equals(actif), avatarVersion);
        }
    }

    /** Identifiants par appel de /interne/collaborateurs/avatars (borne de MS-IAM). */
    static final int LOT_VERSIONS = 200;

    private static final ParameterizedTypeReference<Map<String, String>> VERSIONS = new ParameterizedTypeReference<>() {};

    /** Champs facultatifs : Jackson 3 refuse par défaut une valeur absente pour un type primitif. */
    record PageIam(List<CollaborateurIam> contenu, Long total, Integer page, Integer taille) {}

    private final RestClient client;

    public IamCollaboratorAdapter(RestClient.Builder builder, ServiceTokenInterceptor jeton, @Value("${gimac.services.iam.url}") String url) {
        this.client = builder.baseUrl(url).requestInterceptor(jeton).build();
    }

    @Override
    public PageAnnuaire rechercher(String recherche, int page, int taille) {
        try {
            PageIam p = client
                .get()
                .uri(u -> u.path("/api/v1/interne/collaborateurs").queryParam("recherche", recherche).queryParam("page", page).queryParam("taille", taille).build())
                .retrieve()
                .body(PageIam.class);
            if (p == null || p.contenu() == null) {
                return new PageAnnuaire(List.of(), 0, page, taille);
            }
            List<Collaborateur> contenu = p.contenu().stream().map(CollaborateurIam::vue).toList();
            return new PageAnnuaire(contenu, p.total() == null ? contenu.size() : p.total(), p.page() == null ? page : p.page(), p.taille() == null ? taille : p.taille());
        } catch (RestClientException | IllegalStateException e) {
            throw indisponible();
        }
    }

    @Override
    public Optional<Collaborateur> trouver(String id) {
        try {
            return Optional.ofNullable(client.get().uri("/api/v1/interne/collaborateurs/{id}", id).retrieve().body(CollaborateurIam.class)).map(
                CollaborateurIam::vue
            );
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException | IllegalStateException e) {
            throw indisponible();
        }
    }

    @Override
    public Map<String, String> versionsAvatar(Collection<String> ids) {
        Map<String, String> versions = new HashMap<>();
        List<String> liste = ids == null ? List.of() : ids.stream().filter(i -> i != null && !i.isBlank()).distinct().toList();
        try {
            for (int debut = 0; debut < liste.size(); debut += LOT_VERSIONS) {
                List<String> lot = liste.subList(debut, Math.min(debut + LOT_VERSIONS, liste.size()));
                Map<String, String> r = client
                    .get()
                    .uri(u -> u.path("/api/v1/interne/collaborateurs/avatars").queryParam("ids", lot.toArray()).build())
                    .retrieve()
                    .body(VERSIONS);
                if (r != null) {
                    versions.putAll(r);
                }
            }
        } catch (RestClientException | IllegalStateException e) {
            throw indisponible();
        }
        return versions;
    }

    private static CollaborationException indisponible() {
        return CollaborationException.indisponible("COL_DIRECTORY_UNAVAILABLE", "Annuaire des collaborateurs indisponible");
    }
}
