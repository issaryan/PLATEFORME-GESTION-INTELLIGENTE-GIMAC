package cm.gimac.des.iam.web.rest.lot1;

import cm.gimac.des.iam.application.usecase.sandbox.PerimetresSandboxService;
import cm.gimac.des.iam.application.usecase.sandbox.PerimetresSandboxService.EtatPerimetre;
import cm.gimac.des.iam.application.usecase.sandbox.PerimetresSandboxService.EtatProtection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Étapes des sagas de SBX-ESPACE dans MS-IAM (D-119, docs/lot1/sandbox-existant-adapte.md §6, US-EXI-06) : jeton de
 * service du client sbx-espace portant INTERNAL_IAM_SBX_SAGA. En-tête {@value #CLE} : clé d'idempotence de l'étape
 * (identifiant de saga et rang), rejouable sans double effet.
 */
@RestController
@RequestMapping("/api/v1/interne/sandbox")
@PreAuthorize("hasAuthority('INTERNAL_IAM_SBX_SAGA')")
public class SandboxSagaInterneResource {

    static final String CLE = "Idempotency-Key";

    public record SuspensionRequest(@NotNull Boolean suspendu, @Size(max = 500) String motif) {}

    public record ProtectionRequest(@NotNull Boolean protege) {}

    private final PerimetresSandboxService perimetres;

    public SandboxSagaInterneResource(PerimetresSandboxService perimetres) {
        this.perimetres = perimetres;
    }

    /** Saga SUSPENSION (suspendu vrai) ou RETABLISSEMENT (suspendu faux) : indicateur du périmètre. */
    @PutMapping("/perimetres/{code}/suspension")
    public EtatPerimetre suspension(
        @PathVariable String code,
        @Valid @RequestBody SuspensionRequest r,
        @RequestHeader(name = CLE, required = false) String cle,
        Authentication auth
    ) {
        return perimetres.suspendre(ActeurCourant.de(auth), code, r.suspendu(), r.motif(), cle);
    }

    @GetMapping("/perimetres/{code}")
    public EtatPerimetre perimetre(@PathVariable String code) {
        return perimetres.lire(code);
    }

    /** Attribut de protection du dernier administrateur actif de l'institution. */
    @PutMapping("/comptes/{id}/protection-dernier-administrateur")
    public EtatProtection protection(
        @PathVariable String id,
        @Valid @RequestBody ProtectionRequest r,
        @RequestHeader(name = CLE, required = false) String cle,
        Authentication auth
    ) {
        return perimetres.proteger(ActeurCourant.de(auth), id, r.protege(), cle);
    }
}
