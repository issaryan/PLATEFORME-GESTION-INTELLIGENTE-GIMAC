package cm.gimac.des.iam.web.rest.lot1;

import cm.gimac.des.iam.application.usecase.AffectationsService;
import cm.gimac.des.iam.application.usecase.OrganigrammePdfService;
import cm.gimac.des.iam.application.usecase.OrganisationService;
import cm.gimac.des.iam.domain.organisation.TypeUnite;
import cm.gimac.des.iam.domain.organisation.Unite;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
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
        @Size(max = 36) String responsableId,
        @Size(max = 1000) String motif
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

        /** Motif facultatif, porté au journal (D-102 : gestes de l'organigramme). */
        @Size(max = 1000)
        private String motif;

        public String getMotif() {
            return motif;
        }

        public void setMotif(String motif) {
            this.motif = motif;
        }

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

    /**
     * Affectation de personnes à une unité (D-102, docs/lot1/organigramme.md §2) : 1 à 50 identifiants, motif obligatoire
     * (3 à 500 caractères, contrôlé par le service : 400 IAM_REASON_REQUIRED). {@code responsableId} absent : n+1 résolu
     * pour chaque personne ; null : aucun n+1 ; identifiant : ce n+1 pour tous. Classe à accesseurs pour distinguer
     * « absent » de « null ».
     */
    public static final class AffectationRequest {

        @NotNull
        @Size(min = 1, max = AffectationsService.AFFECTATION_MAX)
        private List<@NotBlank @Size(max = 36) String> comptes;

        @Size(max = 36)
        private String responsableId;

        private boolean responsableFourni;

        @Size(max = 1000)
        private String motif;

        public List<String> getComptes() {
            return comptes;
        }

        public void setComptes(List<String> comptes) {
            this.comptes = comptes;
        }

        public String getResponsableId() {
            return responsableId;
        }

        /** Appelé pour toute valeur présente dans le corps, null compris. */
        public void setResponsableId(String responsableId) {
            this.responsableId = responsableId;
            this.responsableFourni = true;
        }

        public String getMotif() {
            return motif;
        }

        public void setMotif(String motif) {
            this.motif = motif;
        }

        boolean responsableFourni() {
            return responsableFourni;
        }
    }

    private final OrganisationService service;
    private final AffectationsService affectations;
    private final OrganigrammePdfService organigramme;

    public OrganisationResource(OrganisationService service, AffectationsService affectations, OrganigrammePdfService organigramme) {
        this.service = service;
        this.affectations = affectations;
        this.organigramme = organigramme;
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
        Unite u = service.creerUnite(ActeurCourant.de(auth), r.unite(), r.motif());
        return ResponseEntity.created(URI.create("/api/v1/organisation/unites/" + u.code())).body(u);
    }

    @PutMapping("/unites/{code}")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_EDIT')")
    public Unite modifier(@PathVariable String code, @Valid @RequestBody ModificationUniteRequest r, Authentication auth) {
        Unite u = new Unite(code, r.getLibelle(), r.getType(), r.getParent(), r.getActive(), r.getResponsableId());
        return service.modifierUnite(ActeurCourant.de(auth), code, u, !r.responsableFourni(), r.getMotif());
    }

    /**
     * Affecte des personnes à une unité (D-102 §2) : administration (IAM_ACCOUNT_EDIT) ou chef dans le périmètre de son
     * action RATTACHEMENT_MODIFIER (PIL_ACTIVITY_OWN, contrôle par le service). Réponse : avant et après pour chacun.
     */
    @PostMapping("/unites/{code}/affectations")
    @PreAuthorize("hasAnyAuthority('IAM_ACCOUNT_EDIT', 'PIL_ACTIVITY_OWN')")
    public List<AffectationsService.Resultat> affecter(@PathVariable String code, @Valid @RequestBody AffectationRequest r, Authentication auth) {
        return affectations.affecter(ActeurCourant.de(auth), code, r.getComptes(), r.responsableFourni(), r.getResponsableId(), r.getMotif());
    }

    /** Retire une personne de l'unité (D-102 §2), motif obligatoire en paramètre comme la suppression d'un compte. */
    @DeleteMapping("/unites/{code}/affectations/{id}")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_EDIT')")
    public AffectationsService.Resultat retirer(@PathVariable String code, @PathVariable String id, @RequestParam(name = "motif", required = false) String motif, Authentication auth) {
        return affectations.retirer(ActeurCourant.de(auth), code, id, motif);
    }

    /**
     * Organigramme en PDF (D-102 §4), mêmes lecteurs que le catalogue : toute l'organisation ou la branche {@code racine},
     * papier A3 (par défaut) ou A4, paysage (par défaut) ou portrait, annexe des membres avec {@code membres=true}.
     * Moteur indisponible : 503 ORG_PDF_INDISPONIBLE.
     */
    @GetMapping("/organigramme.pdf")
    @PreAuthorize("hasAnyAuthority('PIL_ACTIVITY_OWN', 'IAM_ACCOUNT_READ')")
    public ResponseEntity<byte[]> organigrammePdf(
        @RequestParam(name = "racine", required = false) String racine,
        @RequestParam(name = "format", required = false) String format,
        @RequestParam(name = "orientation", required = false) String orientation,
        @RequestParam(name = "membres", defaultValue = "false") boolean membres
    ) {
        OrganigrammePdfService.Fichier f = organigramme.produire(OrganigrammePdfService.demande(racine, format, orientation, membres));
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(f.nom()).build().toString())
            .cacheControl(CacheControl.noStore())
            .body(f.contenu());
    }

    /** Rattachement du compte connecté (sujet du jeton). */
    @GetMapping("/moi")
    @PreAuthorize("hasAuthority('PIL_ACTIVITY_OWN')")
    public OrganisationService.Rattachement moi(Authentication auth) {
        return service.rattachement(ActeurCourant.de(auth).id());
    }
}
