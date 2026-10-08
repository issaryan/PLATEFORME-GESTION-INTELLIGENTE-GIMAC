package cm.gimac.des.pilotage.infrastructure.client;

import cm.gimac.des.pilotage.application.port.out.OrganisationPort;
import cm.gimac.des.pilotage.domain.exception.PilotageException;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
 * GET /api/v1/interne/organisation/{id}/equipe et GET /api/v1/interne/collaborateurs/{id} ; pouvoirs
 * (docs/lot1/pouvoirs-hierarchiques.md §2, §3, D-81) : GET /api/v1/interne/pouvoirs/{id} et
 * GET /api/v1/interne/organisation/{id}/perimetre?portee= ; processus d'unité (D-87) :
 * GET /api/v1/interne/organisation/unites/{code} (libellé, état et responsable de l'unité, même sans membre ;
 * inconnue : 404 ORG_UNITE_INTROUVABLE), GET /api/v1/interne/organisation/unites/{code}/membres?sousUnites=true&profil=
 * (membres actifs d'une unité avec leurs profils) et profils d'un compte (GET /api/v1/interne/collaborateurs/{id}).
 * Réponses mises en cache 60 s
 * (gimac.organisation.cache-secondes) ; MS-IAM injoignable : 503 PIL_ORGANISATION_INDISPONIBLE, rien
 * n'est supposé ; action ou portée inconnue : aucun pouvoir.
 */
@Component
@ConditionalOnProperty(name = "gimac.adaptateurs.organisation", havingValue = "real")
public class IamOrganisationAdapter implements OrganisationPort {

    record UniteIam(String code, String libelle, String type, String parent, Boolean active, String responsableId) {}

    record ResponsableIam(String id, String nomAffiche, String fonction) {}

    record RattachementIam(String id, String nomAffiche, UniteIam unite, List<UniteIam> chemin, ResponsableIam responsable, List<String> chaine, Integer equipeDirecte) {}

    record MembreIam(String id, String nomAffiche, String fonction, String unite, String responsableId, Integer niveau, Boolean actif) {}

    record CollaborateurIam(String id, String nomAffiche, Set<String> profils, String institutionId, Boolean actif, String fonction, String unite) {}

    record PouvoirsIam(Map<String, String> actions, List<String> profilsDemandables, List<MembreIam> subordonnes, List<String> unitesDirigees) {}

    record PerimetreIam(String portee, List<MembreIam> comptes, List<String> unites) {}

    record MembreProfilIam(String id, String nomAffiche, String fonction, String unite, String responsableId, Boolean actif, Set<String> profils) {}

    private static final ParameterizedTypeReference<List<MembreProfilIam>> MEMBRES = new ParameterizedTypeReference<>() {};

    private record ClePerimetre(String compteId, Portee portee) {}

    private record CleMembres(String unite, String profil) {}

    private static final ParameterizedTypeReference<List<MembreIam>> EQUIPE = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<CollaborateurIam>> COLLABORATEURS = new ParameterizedTypeReference<>() {};

    private final RestClient client;
    private final CacheExpirant<String, Optional<Rattachement>> rattachements;
    private final CacheExpirant<String, List<Membre>> equipes;
    private final CacheExpirant<String, Optional<Personne>> collaborateurs;
    private final CacheExpirant<String, Pouvoirs> pouvoirs;
    private final CacheExpirant<ClePerimetre, Perimetre> perimetres;
    private final CacheExpirant<CleMembres, Optional<List<MembreProfil>>> membresUnites;
    private final CacheExpirant<String, Optional<UniteIam>> unites;
    private final CacheExpirant<String, Set<String>> profils;
    private final CacheExpirant<String, List<MembreProfil>> porteursPermissions;
    private final CacheExpirant<String, List<MembreProfil>> porteursProfils;

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
        this.pouvoirs = new CacheExpirant<>(duree);
        this.perimetres = new CacheExpirant<>(duree);
        this.membresUnites = new CacheExpirant<>(duree);
        this.unites = new CacheExpirant<>(duree);
        this.profils = new CacheExpirant<>(duree);
        this.porteursPermissions = new CacheExpirant<>(duree);
        this.porteursProfils = new CacheExpirant<>(duree);
    }

    /**
     * Unité lue d'abord par GET /api/v1/interne/organisation/unites/{code} (libellé, état, responsable ; inconnue :
     * vide), puis ses membres actifs (sous-unités comprises) par GET /api/v1/interne/organisation/unites/{code}/membres
     * ?sousUnites=true[&profil=] : une unité connue sans membre rend ainsi son chef et une liste vide. MS-IAM rend
     * une liste vide pour un profil inconnu : le profil est alors tenu pour connu (sans porteur).
     */
    @Override
    public Optional<UniteMembres> membresUnite(String unite, String profil) {
        if (unite == null || unite.isBlank()) {
            return Optional.empty();
        }
        Optional<UniteIam> lue = unites.lire(unite, this::lireUnite);
        if (lue.isEmpty()) {
            return Optional.empty();
        }
        UniteIam u = lue.orElseThrow();
        String p = profil == null || profil.isBlank() ? null : profil;
        Optional<List<MembreProfil>> membres = membresUnites.lire(new CleMembres(unite, p), this::lireMembresUnite);
        if (membres.isEmpty()) {
            return Optional.empty();
        }
        String libelle = u.libelle() == null || u.libelle().isBlank() ? unite : u.libelle();
        String chef = u.responsableId() == null || u.responsableId().isBlank() ? null : u.responsableId();
        return Optional.of(new UniteMembres(unite, libelle, chef, Boolean.TRUE.equals(u.active()), true, membres.orElseThrow()));
    }

    /**
     * GET /api/v1/interne/collaborateurs?profil= : comptes actifs du profil (100 au plus), internes seulement, avec leur
     * unité de rattachement (désignation hors de l'unité) ; mis en cache comme les autres lectures.
     */
    @Override
    public List<MembreProfil> porteursProfil(String profil) {
        if (profil == null || profil.isBlank()) {
            return List.of();
        }
        return porteursProfils.lire(profil, this::lirePorteursProfil);
    }

    private List<MembreProfil> lirePorteursProfil(String profil) {
        try {
            List<CollaborateurIam> l = client.get().uri("/api/v1/interne/collaborateurs?profil={profil}", profil).retrieve().body(COLLABORATEURS);
            return l == null
                ? List.of()
                : l
                    .stream()
                    .filter(c -> c != null && c.id() != null && Boolean.TRUE.equals(c.actif()) && (c.institutionId() == null || c.institutionId().isBlank()))
                    .map(c -> new MembreProfil(c.id(), c.nomAffiche(), c.fonction(), vide(c.unite()), true, c.profils() == null ? Set.of() : Set.copyOf(sansVides(List.copyOf(c.profils())))))
                    .toList();
        } catch (HttpClientErrorException.NotFound e) {
            return List.of();
        } catch (RestClientException | IllegalStateException e) {
            throw indisponible();
        }
    }

    /**
     * GET /api/v1/interne/collaborateurs?permission= : comptes actifs dont un profil compose la permission (D-94),
     * internes seulement ; mis en cache comme les autres lectures. 404 : aucun porteur ; autre erreur : 503.
     */
    @Override
    public List<MembreProfil> porteursPermission(String permission) {
        if (permission == null || permission.isBlank()) {
            return List.of();
        }
        return porteursPermissions.lire(permission, this::lirePorteursPermission);
    }

    private List<MembreProfil> lirePorteursPermission(String permission) {
        try {
            List<CollaborateurIam> l = client.get().uri("/api/v1/interne/collaborateurs?permission={permission}", permission).retrieve().body(COLLABORATEURS);
            return l == null
                ? List.of()
                : l
                    .stream()
                    .filter(c -> c != null && c.id() != null && Boolean.TRUE.equals(c.actif()) && (c.institutionId() == null || c.institutionId().isBlank()))
                    .map(c -> new MembreProfil(c.id(), c.nomAffiche(), c.fonction(), vide(c.unite()), true, c.profils() == null ? Set.of() : Set.copyOf(sansVides(List.copyOf(c.profils())))))
                    .toList();
        } catch (HttpClientErrorException.NotFound e) {
            return List.of();
        } catch (RestClientException | IllegalStateException e) {
            throw indisponible();
        }
    }

    /** Changement d'organisation ou de profils reçu de MS-IAM (D-93) : toutes les lectures en cache sont oubliées. */
    @Override
    public void rafraichir() {
        rattachements.vider();
        equipes.vider();
        collaborateurs.vider();
        pouvoirs.vider();
        perimetres.vider();
        membresUnites.vider();
        unites.vider();
        profils.vider();
        porteursPermissions.vider();
        porteursProfils.vider();
    }

    @Override
    public Set<String> profilsDe(String compteId) {
        if (compteId == null || compteId.isBlank()) {
            return Set.of();
        }
        return profils.lire(compteId, this::lireProfils);
    }

    private Optional<List<MembreProfil>> lireMembresUnite(CleMembres cle) {
        try {
            List<MembreProfilIam> r = cle.profil() == null
                ? client.get().uri("/api/v1/interne/organisation/unites/{code}/membres?sousUnites=true", cle.unite()).retrieve().body(MEMBRES)
                : client.get().uri("/api/v1/interne/organisation/unites/{code}/membres?sousUnites=true&profil={profil}", cle.unite(), cle.profil()).retrieve().body(MEMBRES);
            if (r == null) {
                return Optional.of(List.of());
            }
            return Optional.of(
                r
                    .stream()
                    .filter(m -> m != null && m.id() != null)
                    .map(m -> new MembreProfil(m.id(), m.nomAffiche(), m.fonction(), m.unite(), !Boolean.FALSE.equals(m.actif()), m.profils() == null ? Set.of() : Set.copyOf(sansVides(List.copyOf(m.profils())))))
                    .toList()
            );
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException | IllegalStateException e) {
            throw indisponible();
        }
    }

    /** GET /api/v1/interne/organisation/unites/{code} : 404 ORG_UNITE_INTROUVABLE : vide ; autre erreur : 503. */
    private Optional<UniteIam> lireUnite(String code) {
        try {
            return Optional.ofNullable(client.get().uri("/api/v1/interne/organisation/unites/{code}", code).retrieve().body(UniteIam.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException | IllegalStateException e) {
            throw indisponible();
        }
    }

    private Set<String> lireProfils(String id) {
        try {
            CollaborateurIam c = client.get().uri("/api/v1/interne/collaborateurs/{id}", id).retrieve().body(CollaborateurIam.class);
            return c == null || c.profils() == null ? Set.of() : Set.copyOf(sansVides(List.copyOf(c.profils())));
        } catch (HttpClientErrorException.NotFound e) {
            return Set.of();
        } catch (RestClientException | IllegalStateException e) {
            throw indisponible();
        }
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

    @Override
    public Pouvoirs pouvoirs(String compteId) {
        return pouvoirs.lire(compteId, this::lirePouvoirs);
    }

    @Override
    public Perimetre perimetre(String compteId, Portee portee) {
        if (portee == null || portee == Portee.AUCUNE) {
            return Perimetre.vide();
        }
        return perimetres.lire(new ClePerimetre(compteId, portee), this::lirePerimetre);
    }

    private Pouvoirs lirePouvoirs(String id) {
        try {
            PouvoirsIam p = client.get().uri("/api/v1/interne/pouvoirs/{id}", id).retrieve().body(PouvoirsIam.class);
            if (p == null) {
                return Pouvoirs.aucun();
            }
            Map<Pouvoir, Portee> actions = new EnumMap<>(Pouvoir.class);
            if (p.actions() != null) {
                p.actions().forEach((action, portee) -> pouvoir(action).ifPresent(a -> actions.put(a, Portee.lire(portee))));
            }
            return new Pouvoirs(actions, sansVides(p.profilsDemandables()), membres(p.subordonnes()), sansVides(p.unitesDirigees()));
        } catch (HttpClientErrorException.NotFound e) {
            return Pouvoirs.aucun();
        } catch (RestClientException | IllegalStateException e) {
            throw indisponible();
        }
    }

    private Perimetre lirePerimetre(ClePerimetre cle) {
        try {
            PerimetreIam p = client
                .get()
                .uri("/api/v1/interne/organisation/{id}/perimetre?portee={portee}", cle.compteId(), cle.portee().name())
                .retrieve()
                .body(PerimetreIam.class);
            if (p == null) {
                return Perimetre.vide();
            }
            return new Perimetre(p.portee() == null ? cle.portee() : Portee.lire(p.portee()), membres(p.comptes()), sansVides(p.unites()));
        } catch (HttpClientErrorException.NotFound e) {
            return Perimetre.vide();
        } catch (RestClientException | IllegalStateException e) {
            throw indisponible();
        }
    }

    private static Optional<Pouvoir> pouvoir(String action) {
        if (action == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Pouvoir.valueOf(action.strip()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static String vide(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static List<String> sansVides(List<String> l) {
        return l == null ? List.of() : l.stream().filter(Objects::nonNull).filter(x -> !x.isBlank()).toList();
    }

    private static List<Membre> membres(List<MembreIam> l) {
        if (l == null) {
            return List.of();
        }
        return l
            .stream()
            .filter(m -> m != null && m.id() != null)
            .map(m -> new Membre(m.id(), m.nomAffiche(), m.fonction(), m.unite(), m.responsableId(), m.niveau() == null ? 0 : m.niveau(), !Boolean.FALSE.equals(m.actif())))
            .toList();
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
            String unite = r.unite() == null ? null : r.unite().code();
            List<String> chemin = r.chemin() == null ? List.of() : r.chemin().stream().filter(Objects::nonNull).map(UniteIam::code).filter(Objects::nonNull).toList();
            if (chemin.isEmpty() && unite != null) {
                chemin = List.of(unite);
            }
            return Optional.of(new Rattachement(r.id(), r.nomAffiche(), unite, responsable, r.chaine(), chemin));
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
