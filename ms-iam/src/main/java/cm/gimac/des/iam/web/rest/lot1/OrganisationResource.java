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

    /**
     * Création d'une unité ; le code est fixé à la création. Responsable (D-81 §2) : compte interne actif,
     * facultatif.
     */
    public record UniteRequest(
        @NotBlank @Size(max = 32) String code,
        @NotBlank @Size(max = 120) String libelle,
        @NotNull TypeUnite type,
        @Size(max = 32) String parent,
        Boolean active,
        @Size(max = 36) String responsableId
    ) {
        Unite unite() {
            return new Unite(code.trim(), libelle, type, parent, active == null || active, responsableId);
        }
    }

    /**
     * Modification d'une unité : libellé, type, parent, état, responsable ; le code est celui du chemin.
     * {@code responsableId} absent : responsable actuel conservé ; null : retiré ; identifiant : compte
     * interne actif (D-81 §2). Classe à accesseurs (et non record) pour distinguer « absent » de « null ».
     */
    public static final class ModificationUniteRequest {

        @NotBlank
        @Size(max = 120)
        private String libelle;

        @NotNull
        private TypeUnite type;

        @Size(max = 32)
        private String parent;

        @NotNull
        private Boolean active;

        @Size(max = 36)
        private String responsableId;

        private boolean responsableFourni;

        public String getLibelle() {
            return libelle;
        }

        public void setLibelle(String libelle) {
            this.libelle = libelle;
        }

        public TypeUnite getType() {
            return type;
        }

        public void setType(TypeUnite type) {
            this.type = type;
        }

        public String getParent() {
            return parent;
        }

        public void setParent(String parent) {
            this.parent = parent;
        }

        public Boolean getActive() {
            return active;
        }

        public void setActive(Boolean active) {
            this.active = active;
        }

        public String getResponsableId() {
            return responsableId;
        }

        /** Appelé pour toute valeur présente dans le corps, null compris. */
        public void setResponsableId(String responsableId) {
            this.responsableId = responsableId;
            this.responsableFourni = true;
        }

        boolean responsableFourni() {
            return responsableFourni;
        }
    }

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
        Unite u = new Unite(code, r.getLibelle(), r.getType(), r.getParent(), r.getActive(), r.getResponsableId());
        return service.modifierUnite(ActeurCourant.de(auth), code, u, !r.responsableFourni());
    }

    /** Rattachement du compte connecté (sujet du jeton). */
    @GetMapping("/moi")
    @PreAuthorize("hasAuthority('PIL_ACTIVITY_OWN')")
    public OrganisationService.Rattachement moi(Authentication auth) {
        return service.rattachement(ActeurCourant.de(auth).id());
    }
}
