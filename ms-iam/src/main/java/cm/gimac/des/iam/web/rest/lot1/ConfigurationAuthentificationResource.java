package cm.gimac.des.iam.web.rest.lot1;

import cm.gimac.des.iam.application.usecase.ConfigurationAuthentificationService;
import cm.gimac.des.iam.application.usecase.ProfilsService;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.ConfigurationMessagerie;
import cm.gimac.des.iam.domain.compte.PolitiqueSecurite;
import cm.gimac.des.iam.domain.compte.Profil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Configuration de l'authentification depuis la console (D-62), sous /bff/console/iam/configuration :
 * politique de sécurité, messagerie des invitations, profils et leurs permissions, création et suppression
 * de profils (D-81 §5), libellé et description des profils (D-86 §2). Le département
 * (D-69, /bff/des/iam/configuration) n'atteint que la politique, les profils et les permissions.
 */
@RestController
@RequestMapping("/api/v1/configuration")
public class ConfigurationAuthentificationResource {

    public record PolitiqueRequest(@NotNull @Valid PolitiqueSecurite politique, @NotBlank @Size(max = 1000) String motif) {}

    public record MessagerieRequest(@NotNull @Valid ConfigurationMessagerie messagerie, @NotBlank @Size(max = 1000) String motif) {}

    public record TestRequest(@NotBlank @Email String destinataire) {}

    public record PermissionsRequest(@NotNull Set<String> permissions, @NotBlank @Size(max = 1000) String motif) {}

    /**
     * Profil créé par l'administration (D-81 §5) ; libellé facultatif (D-86 §2, absent : le code), motif
     * facultatif, porté au journal.
     */
    public record CreationProfilRequest(
        @NotBlank @Size(max = 60) String code,
        @Size(max = 200) String libelle,
        @NotBlank @Size(max = 1000) String description,
        @NotNull Set<String> permissions,
        @Size(max = 1000) String motif
    ) {}

    /**
     * Libellé (1..80) et description (0..500) d'un profil (D-86 §2), motif obligatoire ; bornes vérifiées
     * par le cas d'usage après normalisation (400 IAM_PROFIL_INVALIDE).
     */
    public record PresentationRequest(@Size(max = 200) String libelle, @Size(max = 1000) String description, @NotBlank @Size(max = 1000) String motif) {}

    private final ConfigurationAuthentificationService service;
    private final ProfilsService profils;
    private final ReauthAdministration reauth;

    public ConfigurationAuthentificationResource(ConfigurationAuthentificationService service, ProfilsService profils, ReauthAdministration reauth) {
        this.service = service;
        this.profils = profils;
        this.reauth = reauth;
    }

    @GetMapping("/politique-securite")
    @PreAuthorize("hasAuthority('PLATFORM_SECURITY_POLICY_READ')")
    public PolitiqueSecurite politique() {
        return service.politique();
    }

    @PutMapping("/politique-securite")
    @PreAuthorize("hasAuthority('PLATFORM_SECURITY_POLICY_EDIT')")
    public PolitiqueSecurite appliquerPolitique(@Valid @RequestBody PolitiqueRequest r, @RequestHeader(name = ComptesResource.REAUTH, required = false) String assertion, Authentication auth) {
        Acteur a = ActeurCourant.de(auth);
        reauth.exiger(a, assertion);
        return service.appliquerPolitique(a, r.politique(), r.motif());
    }

    @GetMapping("/messagerie")
    @PreAuthorize("hasAuthority('PLATFORM_MESSAGING_EDIT')")
    public ConfigurationMessagerie messagerie() {
        return service.messagerie();
    }

    @PutMapping("/messagerie")
    @PreAuthorize("hasAuthority('PLATFORM_MESSAGING_EDIT')")
    public ConfigurationMessagerie appliquerMessagerie(@Valid @RequestBody MessagerieRequest r, Authentication auth) {
        return service.appliquerMessagerie(ActeurCourant.de(auth), r.messagerie(), r.motif());
    }

    @PostMapping("/messagerie/test")
    @PreAuthorize("hasAuthority('PLATFORM_MESSAGING_EDIT')")
    public ResponseEntity<Void> tester(@Valid @RequestBody TestRequest r) {
        service.testerMessagerie(r.destinataire());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/profils")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_READ')")
    public List<Profil> profils() {
        return service.profils();
    }

    /** Création d'un profil (rôle composite) avec ses permissions (D-81 §5) : 201, le profil créé. */
    @PostMapping("/profils")
    @PreAuthorize("hasAuthority('IAM_PROFILE_EDIT')")
    public ResponseEntity<Profil> creerProfil(@Valid @RequestBody CreationProfilRequest r, @RequestHeader(name = ComptesResource.REAUTH, required = false) String assertion, Authentication auth) {
        Acteur a = ActeurCourant.de(auth);
        reauth.exiger(a, assertion);
        Profil p = profils.creer(a, r.code().trim(), r.libelle(), r.description(), r.permissions(), r.motif());
        return ResponseEntity.created(URI.create("/api/v1/configuration/profils/" + p.code())).body(p);
    }

    /**
     * Libellé et description d'un profil livré ou créé (D-86, docs/lot1/profils-organisation.md §2) : le
     * code est immuable ; un profil privilégié est réservé au droit d'octroi privilégié. Réponse : le profil.
     */
    @PutMapping("/profils/{code}")
    @PreAuthorize("hasAuthority('IAM_PROFILE_EDIT')")
    public Profil modifierProfil(
        @PathVariable String code,
        @Valid @RequestBody PresentationRequest r,
        @RequestHeader(name = ComptesResource.REAUTH, required = false) String assertion,
        Authentication auth
    ) {
        Acteur a = ActeurCourant.de(auth);
        reauth.exiger(a, assertion);
        return profils.modifier(a, code, r.libelle(), r.description(), r.motif());
    }

    /** Suppression d'un profil créé, sans titulaire (D-81 §5) ; un profil livré : 409 IAM_PROFIL_SYSTEME. */
    @DeleteMapping("/profils/{code}")
    @PreAuthorize("hasAuthority('IAM_PROFILE_EDIT')")
    public ResponseEntity<Void> supprimerProfil(
        @PathVariable String code,
        @RequestParam(name = "motif", required = false) String motif,
        @RequestHeader(name = ComptesResource.REAUTH, required = false) String assertion,
        Authentication auth
    ) {
        Acteur a = ActeurCourant.de(auth);
        reauth.exiger(a, assertion);
        profils.supprimer(a, code, motif);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_READ')")
    public List<String> permissions() {
        return service.permissions();
    }

    @PutMapping("/profils/{code}/permissions")
    @PreAuthorize("hasAuthority('IAM_PROFILE_EDIT')")
    public Profil definirPermissions(
        @PathVariable String code,
        @Valid @RequestBody PermissionsRequest r,
        @RequestHeader(name = ComptesResource.REAUTH, required = false) String assertion,
        Authentication auth
    ) {
        Acteur a = ActeurCourant.de(auth);
        reauth.exiger(a, assertion);
        return service.definirPermissions(a, code, r.permissions(), r.motif());
    }
}
