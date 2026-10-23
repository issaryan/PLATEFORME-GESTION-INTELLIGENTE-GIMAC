package cm.gimac.des.iam.web.rest.lot1;

import cm.gimac.des.iam.application.usecase.sandbox.DelegationsSandboxService;
import cm.gimac.des.iam.application.usecase.sandbox.DelegationsSandboxService.PageDelegations;
import cm.gimac.des.iam.application.usecase.sandbox.PerimetresSandboxService;
import cm.gimac.des.iam.domain.sandbox.Delegation.TypeDelegation;
import cm.gimac.des.iam.application.usecase.sandbox.PerimetresSandboxService.EtatPerimetre;
import cm.gimac.des.iam.application.usecase.sandbox.PerimetresSandboxService.PagePerimetres;
import cm.gimac.des.iam.application.usecase.sandbox.PerimetresSandboxService.PageProtections;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sandbox GIMAC dans la console de la plateforme (D-119, docs/lot1/sandbox-existant-adapte.md §6) : périmètres
 * d'institution suspendus, comptes protégés et délégations, en lecture (IAM_ACCOUNT_READ) ; suspensions et protections sont posées
 * par les sagas de SBX-ESPACE seulement.
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
