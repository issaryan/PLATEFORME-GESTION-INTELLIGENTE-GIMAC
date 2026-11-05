package cm.gimac.des.participant.web.rest.lot1;

import cm.gimac.des.participant.application.port.in.RepriseCorrespondantsUseCase;
import cm.gimac.des.participant.application.port.in.RepriseCorrespondantsUseCase.CorrespondantRepris;
import cm.gimac.des.participant.application.port.in.RepriseCorrespondantsUseCase.Rapport;
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
 * Reprise des correspondants des institutions (D-132, docs/exploitation/reprise-des-donnees.md §3 bis), permission
 * REF_REPRISE_IMPORT : route interne pour le compte de service reprise-service, route métier relayée par la passerelle
 * pour le super administrateur (jeton à deux facteurs). Sans « simulation » explicite à false, rien n'est écrit.
 */
@RestController
@RequestMapping({ "/api/v1/interne/reprise", "/api/v1/reprise" })
public class RepriseCorrespondantsResource {

    /** Lot à reprendre ; {@code simulation} absente vaut simulation. */
    public record RepriseRequest(Boolean simulation, @Size(max = 200) String motif, @NotEmpty @Size(max = RepriseCorrespondantsUseCase.LIGNES_MAX) List<CorrespondantRepris> correspondants) {}

    private final RepriseCorrespondantsUseCase reprise;

    public RepriseCorrespondantsResource(RepriseCorrespondantsUseCase reprise) {
        this.reprise = reprise;
    }

    @PostMapping("/correspondants")
    @PreAuthorize("hasAuthority('REF_REPRISE_IMPORT')")
    public Rapport importer(@Valid @RequestBody RepriseRequest r, Authentication auth) {
        return reprise.importer(r.correspondants(), !Boolean.FALSE.equals(r.simulation()), r.motif(), ActeurCourant.de(auth));
    }
}
