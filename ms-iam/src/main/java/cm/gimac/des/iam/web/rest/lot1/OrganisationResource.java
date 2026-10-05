package cm.gimac.des.iam.web.rest.lot1;

import cm.gimac.des.iam.application.usecase.OrganisationService;
import cm.gimac.des.iam.domain.organisation.TypeUnite;
import cm.gimac.des.iam.domain.organisation.Unite;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Organisation du GIMAC (D-76, docs/lot1/mon-activite.md §1.3), exposée sous
 * /bff/{des,console}/iam/organisation : catalogue des unités (lecture pour « Mon activité » et
 * l'administration, édition par l'administration) et rattachement du compte connecté.
 */
@RestController
@RequestMapping("/api/v1/organisation")
public class OrganisationResource {

    /** Création d'une unité ; le code est fixé à la création. */
    public record UniteRequest(
        @NotBlank @Size(max = 32) String code,
        @NotBlank @Size(max = 120) String libelle,
        @NotNull TypeUnite type,
        @Size(max = 32) String parent,
        Boolean active
    ) {
        Unite unite() {
            return new Unite(code.trim(), libelle, type, parent, active == null || active);
        }
    }

    /** Modification d'une unité : libellé, type, parent, état ; le code est celui du chemin. */
    public record ModificationUniteRequest(@NotBlank @Size(max = 120) String libelle, @NotNull TypeUnite type, @Size(max = 32) String parent, @NotNull Boolean active) {}

    private final OrganisationService service;

    public OrganisationResource(OrganisationService service) {
        this.service = service;
    }

    /** Unités du catalogue, triées par code ; « actives=true » écarte les unités inactives. */
    @GetMapping("/unites")
    @PreAuthorize("hasAnyAuthority('PIL_ACTIVITY_OWN', 'IAM_ACCOUNT_READ')")
    public List<Unite> unites(@RequestParam(name = "actives", defaultValue = "false") boolean actives) {
        return service.unites(actives);
    }

    @PostMapping("/unites")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_EDIT')")
    public ResponseEntity<Unite> creer(@Valid @RequestBody UniteRequest r, Authentication auth) {
        Unite u = service.creerUnite(ActeurCourant.de(auth), r.unite());
        return ResponseEntity.created(URI.create("/api/v1/organisation/unites/" + u.code())).body(u);
    }

    @PutMapping("/unites/{code}")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_EDIT')")
    public Unite modifier(@PathVariable String code, @Valid @RequestBody ModificationUniteRequest r, Authentication auth) {
        return service.modifierUnite(ActeurCourant.de(auth), code, new Unite(code, r.libelle(), r.type(), r.parent(), r.active()));
    }

    /** Rattachement du compte connecté (sujet du jeton). */
    @GetMapping("/moi")
    @PreAuthorize("hasAuthority('PIL_ACTIVITY_OWN')")
    public OrganisationService.Rattachement moi(Authentication auth) {
        return service.rattachement(ActeurCourant.de(auth).id());
    }
}
