package cm.gimac.des.homologation.web.rest.lot1;

import cm.gimac.des.homologation.application.port.in.RepriseLiaisonsUseCase;
import cm.gimac.des.homologation.application.port.in.RepriseLiaisonsUseCase.LiaisonReprise;
import cm.gimac.des.homologation.application.port.in.RepriseLiaisonsUseCase.Rapport;
import cm.gimac.des.homologation.domain.parcours.Acteur;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reprise des projets de certification existants (D-107, docs/exploitation/reprise-des-donnees.md §4), permission
 * REF_REPRISE_IMPORT : route interne pour le compte de service reprise-service (outil infra/reprise), route métier
 * relayée par la passerelle pour le super administrateur (jeton à deux facteurs). Sans « simulation » explicite à false,
 * rien n'est écrit.
 */
@RestController
@RequestMapping({ "/api/v1/interne/reprise", "/api/v1/reprise" })
public class RepriseLiaisonsResource {

    /** Lot à reprendre ; {@code simulation} absente vaut simulation. */
    public record RepriseRequest(Boolean simulation, @NotEmpty @Size(max = RepriseLiaisonsUseCase.LIGNES_MAX) List<LiaisonReprise> liaisons) {}

    private final RepriseLiaisonsUseCase reprise;

    public RepriseLiaisonsResource(RepriseLiaisonsUseCase reprise) {
        this.reprise = reprise;
    }

    @PostMapping("/liaisons")
    @PreAuthorize("hasAuthority('REF_REPRISE_IMPORT')")
    public Rapport importer(@Valid @RequestBody RepriseRequest r, Authentication auth) {
        Acteur a = ActeurCourant.de(auth);
        return reprise.importer(r.liaisons(), !Boolean.FALSE.equals(r.simulation()), new RepriseLiaisonsUseCase.Acteur(a.id(), a.nom()));
    }
}
