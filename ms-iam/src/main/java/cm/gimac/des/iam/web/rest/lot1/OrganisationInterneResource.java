package cm.gimac.des.iam.web.rest.lot1;

import cm.gimac.des.iam.application.usecase.OrganisationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Organisation pour les autres services (D-76, docs/lot1/mon-activite.md §1.3 et §3), jeton de service
 * INTERNAL_IAM_READ (hors passerelle) : rattachement d'un compte et son équipe, que MS-PILOTAGE lit
 * pour les règles d'équipe. Aucune donnée de contact ni de sécurité n'y figure.
 */
@RestController
@RequestMapping("/api/v1/interne/organisation")
public class OrganisationInterneResource {

    private final OrganisationService service;

    public OrganisationInterneResource(OrganisationService service) {
        this.service = service;
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
}
