package cm.gimac.des.homologation.web.rest.lot1;

import cm.gimac.des.homologation.application.port.in.RepriseCloturesUseCase;
import cm.gimac.des.homologation.application.port.in.RepriseCloturesUseCase.ClotureReprise;
import cm.gimac.des.homologation.application.port.in.RepriseCloturesUseCase.Rapport;
import cm.gimac.des.homologation.application.port.in.RepriseLiaisonsUseCase;
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
 * Clôture sans suite de liaisons reprises (D-137, docs/exploitation/reprise-des-donnees.md §4 bis), même droit que la
 * reprise (REF_REPRISE_IMPORT) : route interne pour le compte de service reprise-service, route métier relayée par la
 * passerelle pour le super administrateur (jeton à deux facteurs). Motif obligatoire ; sans « simulation » explicite à
 * false, rien n'est écrit.
 */
@RestController
@RequestMapping({ "/api/v1/interne/reprise", "/api/v1/reprise" })
public class RepriseCloturesResource {

    /** Lot à clore ; {@code simulation} absente vaut simulation. */
    public record CloturesRequest(
        Boolean simulation,
        @Size(max = RepriseCloturesUseCase.MOTIF_MAX) String motif,
        @NotEmpty @Size(max = RepriseCloturesUseCase.LIGNES_MAX) List<ClotureReprise> liaisons
    ) {}

    private final RepriseCloturesUseCase clotures;

    public RepriseCloturesResource(RepriseCloturesUseCase clotures) {
        this.clotures = clotures;
    }

    @PostMapping("/liaisons/clotures")
    @PreAuthorize("hasAuthority('REF_REPRISE_IMPORT')")
    public Rapport clore(@Valid @RequestBody CloturesRequest r, Authentication auth) {
        Acteur a = ActeurCourant.de(auth);
        return clotures.clore(r.liaisons(), !Boolean.FALSE.equals(r.simulation()), r.motif(), new RepriseLiaisonsUseCase.Acteur(a.id(), a.nom()));
    }
}
