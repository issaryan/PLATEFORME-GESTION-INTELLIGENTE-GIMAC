package cm.gimac.des.pilotage.infrastructure.client;

import cm.gimac.des.pilotage.application.port.out.OrganisationPort;
import cm.gimac.des.pilotage.domain.exception.PilotageException;
import java.time.Duration;
import java.util.List;
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
 * Adaptateur réel de l'organisation : interfaces internes de MS-IAM (docs/lot1/mon-activite.md §1.3,
 * jeton de service du client pilotage-service, INTERNAL_IAM_READ) : GET /api/v1/interne/organisation/{id},
 * GET /api/v1/interne/organisation/{id}/equipe et GET /api/v1/interne/collaborateurs/{id}. Réponses
 * mises en cache 60 s (gimac.organisation.cache-secondes) ; MS-IAM injoignable : 503
 * PIL_ORGANISATION_INDISPONIBLE, rien n'est supposé.
 */
@Component
@ConditionalOnProperty(name = "gimac.adaptateurs.organisation", havingValue = "real")
public class IamOrganisationAdapter implements OrganisationPort {

    record UniteIam(String code, String libelle, String type, String parent, Boolean active) {}

    record ResponsableIam(String id, String nomAffiche, String fonction) {}

    record RattachementIam(String id, String nomAffiche, UniteIam unite, List<UniteIam> chemin, ResponsableIam responsable, List<String> chaine, Integer equipeDirecte) {}

    record MembreIam(String id, String nomAffiche, String fonction, String unite, String responsableId, Integer niveau, Boolean actif) {}

    record CollaborateurIam(String id, String nomAffiche, Set<String> profils, String institutionId, Boolean actif, String fonction) {}

    private static final ParameterizedTypeReference<List<MembreIam>> EQUIPE = new ParameterizedTypeReference<>() {};

    private final RestClient client;
    private final CacheExpirant<String, Optional<Rattachement>> rattachements;
    private final CacheExpirant<String, List<Membre>> equipes;
    private final CacheExpirant<String, Optional<Personne>> collaborateurs;

    public IamOrganisationAdapter(
        RestClient.Builder builder,
        ServiceTokenInterceptor jeton,
        @Value("${gimac.services.iam.url}") String url,
        @Value("${gimac.organisation.cache-secondes:60}") long cacheSecondes
    ) {
        this.client = builder.baseUrl(url).requestInterceptor(jeton).build();
        Duration duree = Duration.ofSeconds(cacheSecondes);
        this.rattachements = new CacheExpirant<>(duree);
        this.equipes = new CacheExpirant<>(duree);
        this.collaborateurs = new CacheExpirant<>(duree);
    }

    @Override
    public Optional<Rattachement> rattachement(String compteId) {
        return rattachements.lire(compteId, this::lireRattachement);
    }

    @Override
    public List<Membre> equipe(String compteId) {
        return equipes.lire(compteId, this::lireEquipe);
    }

    @Override
    public Optional<Personne> collaborateur(String compteId) {
        return collaborateurs.lire(compteId, this::lireCollaborateur);
    }

    private Optional<Rattachement> lireRattachement(String id) {
        try {
            RattachementIam r = client.get().uri("/api/v1/interne/organisation/{id}", id).retrieve().body(RattachementIam.class);
            if (r == null) {
                return Optional.empty();
            }
            Personne responsable = r.responsable() == null || r.responsable().id() == null
                ? null
                : new Personne(r.responsable().id(), r.responsable().nomAffiche(), r.responsable().fonction(), true, true);
            return Optional.of(
                new Rattachement(r.id(), r.nomAffiche(), r.unite() == null ? null : r.unite().code(), responsable, r.chaine() == null ? List.of() : List.copyOf(r.chaine()))
            );
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException | IllegalStateException e) {
            throw indisponible();
        }
    }

    private List<Membre> lireEquipe(String id) {
        try {
            List<MembreIam> l = client.get().uri("/api/v1/interne/organisation/{id}/equipe", id).retrieve().body(EQUIPE);
            if (l == null) {
                return List.of();
            }
            return l
                .stream()
                .filter(m -> m != null && m.id() != null)
                .map(m ->
                    new Membre(m.id(), m.nomAffiche(), m.fonction(), m.unite(), m.responsableId(), m.niveau() == null ? 1 : m.niveau(), !Boolean.FALSE.equals(m.actif()))
                )
                .toList();
        } catch (HttpClientErrorException.NotFound e) {
            return List.of();
        } catch (RestClientException | IllegalStateException e) {
            throw indisponible();
        }
    }

    private Optional<Personne> lireCollaborateur(String id) {
        try {
            CollaborateurIam c = client.get().uri("/api/v1/interne/collaborateurs/{id}", id).retrieve().body(CollaborateurIam.class);
            if (c == null) {
                return Optional.empty();
            }
            boolean interne = c.institutionId() == null || c.institutionId().isBlank();
            return Optional.of(new Personne(c.id(), c.nomAffiche(), c.fonction(), Boolean.TRUE.equals(c.actif()), interne));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException | IllegalStateException e) {
            throw indisponible();
        }
    }

    private static PilotageException indisponible() {
        return PilotageException.indisponible("PIL_ORGANISATION_INDISPONIBLE", "Organisation (MS-IAM) indisponible");
    }
}
