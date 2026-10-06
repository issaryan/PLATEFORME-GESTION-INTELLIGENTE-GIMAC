package cm.gimac.des.iam.web.rest.lot1;

import cm.gimac.des.iam.application.usecase.OrganisationService;
import cm.gimac.des.iam.domain.organisation.Unite;
import cm.gimac.des.iam.domain.pouvoir.Portee;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Organisation pour les autres services (D-76, docs/lot1/mon-activite.md §1.3 et §3), jeton de service
 * INTERNAL_IAM_READ (hors passerelle) : rattachement d'un compte, son équipe et son périmètre pour une
 * portée (D-81), que MS-PILOTAGE lit pour les règles d'équipe. Aucune donnée de contact ni de sécurité n'y figure.
 */
@RestController
@RequestMapping("/api/v1/interne/organisation")
public class OrganisationInterneResource {

    private final OrganisationService service;

    public OrganisationInterneResource(OrganisationService service) {
        this.service = service;
    }

    /**
     * Une unité par son code (D-87) : {@code { code, libelle, type, parent, active, responsableId }}, active ou
     * non ; le moteur de processus y lit le chef d'une unité, même sans membre. Unité inconnue : 404.
     */
    @GetMapping("/unites/{code}")
    public Unite unite(@PathVariable String code) {
        return service.unite(code);
    }

    /**
     * Membres actifs d'une unité (D-86, moteur de processus D-87) : comptes internes actifs, filtrés par
     * profil s'il est donné, sous-unités comprises par défaut ; 500 au plus. Unité inconnue : 404.
     */
    @GetMapping("/unites/{code}/membres")
    public List<OrganisationService.MembreUnite> membresUnite(
        @PathVariable String code,
        @RequestParam(name = "profil", required = false) String profil,
        @RequestParam(name = "sousUnites", defaultValue = "true") boolean sousUnites
    ) {
        return service.membresUnite(code, profil, sousUnites);
    }

    @GetMapping("/{id}")
    public OrganisationService.Rattachement rattachement(@PathVariable String id) {
        return service.rattachement(id);
    }

    /** Comptes dont {id} figure dans la chaîne des responsables (récursif, 500 au plus), avec leur niveau. */
    @GetMapping("/{id}/equipe")
    public List<OrganisationService.MembreEquipe> equipe(@PathVariable String id) {
        return service.equipe(id);
    }

    /**
     * Périmètre de {id} pour une portée (D-81, docs/lot1/pouvoirs-hierarchiques.md §2) :
     * {@code { portee, comptes: [...], unites: [codes] }}.
     */
    @GetMapping("/{id}/perimetre")
    public OrganisationService.Perimetre perimetre(@PathVariable String id, @RequestParam("portee") Portee portee) {
        return service.perimetre(id, portee);
    }
}
