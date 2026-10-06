package cm.gimac.des.iam.web.rest.lot1;

import cm.gimac.des.iam.application.usecase.AvatarService;
import cm.gimac.des.iam.application.usecase.PouvoirsService;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.pouvoir.ActionPouvoir;
import cm.gimac.des.iam.domain.pouvoir.Portee;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Matrice des pouvoirs (D-81, docs/lot1/pouvoirs-hierarchiques.md §3), exposée sous
 * /bff/{des,console}/iam/pouvoirs : lecture par l'administration, réglage d'un profil (IAM_PROFILE_EDIT et
 * réauthentification FIDO2), pouvoirs du compte connecté.
 */
@RestController
@RequestMapping("/api/v1/pouvoirs")
public class PouvoirsResource {

    /** Ligne de la matrice : portée par action (absente : AUCUNE), profils demandables, motif facultatif. */
    public record LigneRequest(@NotNull Map<ActionPouvoir, Portee> actions, @NotNull Set<String> profilsDemandables, @Size(max = 1000) String motif) {}

    /**
     * Rattachement d'un subordonné par son chef : même corps que PUT /comptes/{id}/rattachement (null ou
     * absent retire la valeur), motif facultatif pour le journal.
     */
    public record RattachementSubordonneRequest(@Size(max = 32) String unite, @Size(max = 36) String responsableId, @Size(max = 1000) String motif) {}

    private final PouvoirsService service;
    private final ReauthAdministration reauth;
    private final AvatarService avatars;

    public PouvoirsResource(PouvoirsService service, ReauthAdministration reauth, AvatarService avatars) {
        this.service = service;
        this.reauth = reauth;
        this.avatars = avatars;
    }

    @GetMapping("/matrice")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_READ')")
    public PouvoirsService.Matrice matrice() {
        return service.matrice();
    }

    @PutMapping("/matrice/{profil}")
    @PreAuthorize("hasAuthority('IAM_PROFILE_EDIT')")
    public PouvoirsService.LigneMatrice definir(
        @PathVariable String profil,
        @Valid @RequestBody LigneRequest r,
        @RequestHeader(name = ComptesResource.REAUTH, required = false) String assertion,
        Authentication auth
    ) {
        Acteur a = ActeurCourant.de(auth);
        reauth.exiger(a, assertion);
        return service.definir(a, profil, r.actions(), r.profilsDemandables(), r.motif());
    }

    /** Pouvoirs du compte connecté : portées, profils demandables, subordonnés couverts, unités dirigées. */
    @GetMapping("/moi")
    @PreAuthorize("hasAuthority('PIL_ACTIVITY_OWN')")
    public PouvoirsService.VuePouvoirs moi(Authentication auth) {
        return service.vue(ActeurCourant.de(auth).id());
    }

    /**
     * Rattachement d'un subordonné par son chef (action RATTACHEMENT_MODIFIER de la matrice, §3) ; réponse :
     * la fiche du compte, comme PUT /comptes/{id}/rattachement.
     */
    @PutMapping("/subordonnes/{id}/rattachement")
    @PreAuthorize("hasAuthority('PIL_ACTIVITY_OWN')")
    public ComptesResource.CompteVue rattacherSubordonne(@PathVariable String id, @Valid @RequestBody RattachementSubordonneRequest r, Authentication auth) {
        return ComptesResource.CompteVue.de(service.rattacherSubordonne(ActeurCourant.de(auth), id, r.unite(), r.responsableId(), r.motif())).avecAvatar(avatars.version(id).orElse(null));
    }
}
