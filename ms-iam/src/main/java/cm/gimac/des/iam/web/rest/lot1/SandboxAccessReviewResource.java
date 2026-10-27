package cm.gimac.des.iam.web.rest.lot1;

import cm.gimac.des.iam.application.usecase.sandbox.ReexamensAccesService;
import cm.gimac.des.iam.application.usecase.sandbox.ReexamensAccesService.Campagne;
import cm.gimac.des.iam.application.usecase.sandbox.ReexamensAccesService.VueCampagne;
import cm.gimac.des.iam.domain.enumeration.ReviewDecision;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Console des campagnes de réexamen des accès (IAM-18, US-EXI-23 critère 2). */
@RestController
@RequestMapping("/api/v1/comptes/reexamens")
public class SandboxAccessReviewResource {

    public record DecisionRequest(@NotNull ReviewDecision decision, Set<@Size(max = 64) String> profils, @Size(max = 500) String comment) {}

    private final ReexamensAccesService service;

    public SandboxAccessReviewResource(ReexamensAccesService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_READ')")
    public List<Campagne> lister() {
        return service.lister();
    }

    @PostMapping("/ouverture")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_CREATE')")
    public Campagne ouvrir(Authentication auth) {
        return service.ouvrirCourante(ActeurCourant.de(auth));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_READ')")
    public VueCampagne lire(@PathVariable UUID id) {
        return service.lire(id);
    }

    @PostMapping("/{id}/comptes/{compteId}/decision")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_ROLES')")
    public VueCampagne decider(@PathVariable UUID id, @PathVariable String compteId, @Valid @RequestBody DecisionRequest request, Authentication auth) {
        return service.decider(ActeurCourant.de(auth), id, compteId, request.decision(), request.profils(), request.comment());
    }

    @PostMapping("/{id}/cloture")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_ROLES')")
    public Campagne cloturer(@PathVariable UUID id, Authentication auth) {
        return service.cloturer(ActeurCourant.de(auth), id);
    }
}
