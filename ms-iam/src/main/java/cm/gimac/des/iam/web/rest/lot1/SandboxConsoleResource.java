package cm.gimac.des.iam.web.rest.lot1;

import cm.gimac.des.iam.application.usecase.sandbox.DelegationsSandboxService;
import cm.gimac.des.iam.application.usecase.sandbox.DelegationsSandboxService.PageDelegations;
import cm.gimac.des.iam.application.usecase.sandbox.DelegationsSandboxService.VueDelegation;
import cm.gimac.des.iam.application.usecase.sandbox.PerimetresSandboxService;
import cm.gimac.des.iam.domain.sandbox.Delegation.TypeDelegation;
import cm.gimac.des.iam.application.usecase.sandbox.PerimetresSandboxService.EtatPerimetre;
import cm.gimac.des.iam.application.usecase.sandbox.PerimetresSandboxService.PagePerimetres;
import cm.gimac.des.iam.application.usecase.sandbox.PerimetresSandboxService.PageProtections;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sandbox GIMAC dans la console de la plateforme (D-119, docs/lot1/sandbox-existant-adapte.md §6) : périmètres
 * d'institution suspendus, comptes protégés et délégations, en lecture (IAM_ACCOUNT_READ) ; suspensions et protections sont posées
 * par les sagas de SBX-ESPACE seulement. Accès de prestataire de maintenance (US-EXI-24) : demande, approbation par le
 * RSSI (quatre yeux), refus et révocation sous IAM_ACCOUNT_SECURITY et second facteur.
 */
@RestController
@RequestMapping("/api/v1/sandbox")
public class SandboxConsoleResource {

    private final PerimetresSandboxService perimetres;
    private final DelegationsSandboxService delegations;

    public SandboxConsoleResource(PerimetresSandboxService perimetres, DelegationsSandboxService delegations) {
        this.perimetres = perimetres;
        this.delegations = delegations;
    }

    /** Délégations (mandats de partenaire, accès de prestataire), filtre par type facultatif. */
    @GetMapping("/delegations")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_READ')")
    public PageDelegations delegations(
        @RequestParam(name = "type", required = false) TypeDelegation type,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "taille", defaultValue = "20") int taille
    ) {
        return delegations.lister(type, page, taille);
    }

    /** Demande d'accès de prestataire : composants techniques désignés, période, motif. */
    public record DemandePrestataireRequest(
        @NotBlank @Size(max = 64) String compteId,
        @NotEmpty @Size(max = 12) List<@NotBlank @Size(max = 32) String> composants,
        @NotNull Instant debut,
        @NotNull Instant fin,
        @NotBlank @Size(max = 500) String motif
    ) {}

    /** Motif d'un refus ou d'une révocation. */
    public record MotifRequest(@NotBlank @Size(max = 500) String motif) {}

    @PostMapping("/delegations/prestataire")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_SECURITY')")
    public ResponseEntity<VueDelegation> demanderPrestataire(@Valid @RequestBody DemandePrestataireRequest r, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(delegations.demanderAccesPrestataire(ActeurCourant.de(auth), r.compteId(), r.composants(), r.debut(), r.fin(), r.motif()));
    }

    @PostMapping("/delegations/{id}/approbation")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_SECURITY')")
    public VueDelegation approuverPrestataire(@PathVariable String id, Authentication auth) {
        return delegations.approuverAccesPrestataire(ActeurCourant.de(auth), id);
    }

    @PostMapping("/delegations/{id}/refus")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_SECURITY')")
    public VueDelegation refuserPrestataire(@PathVariable String id, @Valid @RequestBody MotifRequest r, Authentication auth) {
        return delegations.refuserAccesPrestataire(ActeurCourant.de(auth), id, r.motif());
    }

    @PostMapping("/delegations/{id}/revocation")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_SECURITY')")
    public VueDelegation revoquerPrestataire(@PathVariable String id, @Valid @RequestBody MotifRequest r, Authentication auth) {
        return delegations.revoquerAccesPrestataire(ActeurCourant.de(auth), id, r.motif());
    }

    @GetMapping("/perimetres")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_READ')")
    public PagePerimetres perimetres(@RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "taille", defaultValue = "20") int taille) {
        return perimetres.suspendus(page, taille);
    }

    @GetMapping("/perimetres/{code}")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_READ')")
    public EtatPerimetre perimetre(@PathVariable String code) {
        return perimetres.lire(code);
    }

    @GetMapping("/protections")
    @PreAuthorize("hasAuthority('IAM_ACCOUNT_READ')")
    public PageProtections protections(@RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "taille", defaultValue = "20") int taille) {
        return perimetres.protections(page, taille);
    }
}
