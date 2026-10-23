package cm.gimac.des.iam.web.rest.lot1;

import cm.gimac.des.iam.application.usecase.sandbox.PerimetresSandboxService;
import cm.gimac.des.iam.application.usecase.sandbox.PerimetresSandboxService.EtatPerimetre;
import cm.gimac.des.iam.application.usecase.sandbox.PerimetresSandboxService.EtatProtection;
import cm.gimac.des.iam.application.usecase.sandbox.DelegationsSandboxService;
import cm.gimac.des.iam.application.usecase.sandbox.DelegationsSandboxService.VueDelegation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.time.Instant;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
 * Étapes des sagas de SBX-ESPACE dans MS-IAM (D-119, docs/lot1/sandbox-existant-adapte.md §6, US-EXI-06, US-EXI-04) : jeton de
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

    /** Mandat de partenaire (port accorderDelegation) : droits SBX_<MODULE>_<FONCTION> délégués. */
    public record MandatRequest(
        @NotBlank @Size(max = 100) String compteId,
        @NotBlank @Size(max = 64) String institution,
        @NotNull @Size(max = 61) Set<@NotBlank @Size(max = 64) String> permissions,
        @NotNull Instant debut,
        @NotNull Instant fin
    ) {}

    public record ProlongationRequest(@NotNull Instant fin) {}

    private final PerimetresSandboxService perimetres;
    private final DelegationsSandboxService delegations;

    public SandboxSagaInterneResource(PerimetresSandboxService perimetres, DelegationsSandboxService delegations) {
        this.perimetres = perimetres;
        this.delegations = delegations;
    }

    /** Saga MANDAT : délégation temporaire d'un intervenant ; 201, ou 200 pour une étape rejouée. */
    @PostMapping("/delegations")
    public ResponseEntity<VueDelegation> accorder(@Valid @RequestBody MandatRequest r, @RequestHeader(name = CLE, required = false) String cle, Authentication auth) {
        VueDelegation v = delegations.accorderMandat(ActeurCourant.de(auth), r.compteId(), r.institution(), r.permissions(), r.debut(), r.fin(), cle);
        return ResponseEntity.status(v.rejouee() ? HttpStatus.OK : HttpStatus.CREATED).location(URI.create("/api/v1/interne/sandbox/delegations/" + v.delegation().id())).body(v);
    }

    @GetMapping("/delegations/{id}")
    public VueDelegation delegation(@PathVariable String id) {
        return delegations.lire(id);
    }

    /** Révocation (port revoquerDelegation), immédiate. */
    @DeleteMapping("/delegations/{id}")
    public VueDelegation revoquer(
        @PathVariable String id,
        @RequestParam(name = "motif", required = false) @Size(max = 500) String motif,
        @RequestHeader(name = CLE, required = false) String cle,
        Authentication auth
    ) {
        return delegations.revoquer(ActeurCourant.de(auth), id, motif, cle);
    }

    /** Prolongation d'un mandat : nouvelle fin, même contrôle de durée. */
    @PutMapping("/delegations/{id}/fin")
    public VueDelegation prolonger(@PathVariable String id, @Valid @RequestBody ProlongationRequest r, @RequestHeader(name = CLE, required = false) String cle, Authentication auth) {
        return delegations.prolonger(ActeurCourant.de(auth), id, r.fin(), cle);
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
