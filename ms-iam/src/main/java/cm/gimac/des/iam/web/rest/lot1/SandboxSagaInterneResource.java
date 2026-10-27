package cm.gimac.des.iam.web.rest.lot1;

import cm.gimac.des.iam.application.usecase.sandbox.PerimetresSandboxService;
import cm.gimac.des.iam.application.usecase.sandbox.PerimetresSandboxService.EtatPerimetre;
import cm.gimac.des.iam.application.usecase.sandbox.PerimetresSandboxService.EtatProtection;
import cm.gimac.des.iam.application.usecase.sandbox.ComptesSandboxSagaService;
import cm.gimac.des.iam.application.usecase.sandbox.ComptesSandboxSagaService.CompteSaga;
import cm.gimac.des.iam.application.usecase.sandbox.DelegationsSandboxService;
import cm.gimac.des.iam.application.usecase.sandbox.DelegationsSandboxService.VueDelegation;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
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
 * Étapes des sagas de SBX-ESPACE dans MS-IAM (OUVERTURE/MATRICE, D-119, US-EXI-06, US-EXI-04) : jeton de
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

    /** Ouverture du compte partagé, sans mot de passe rendu au service appelant. */
    public record OuvertureRequest(
        @NotBlank @Size(max = 50) String identifiant,
        @NotBlank @Size(max = 100) String prenom,
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Size(max = 255) String email,
        @Size(max = 20) String telephone,
        @Size(max = 200) String fonction,
        @NotBlank @Size(max = 64) String institutionId,
        @NotNull Set<@NotBlank @Size(max = 64) String> profils
    ) {}

    public record MatriceRequest(@NotNull Set<@NotBlank @Size(max = 64) String> profils, @NotBlank @Size(max = 500) String motif) {}

    private final PerimetresSandboxService perimetres;
    private final DelegationsSandboxService delegations;
    private final ComptesSandboxSagaService comptes;

    public SandboxSagaInterneResource(PerimetresSandboxService perimetres, DelegationsSandboxService delegations, ComptesSandboxSagaService comptes) {
        this.perimetres = perimetres;
        this.delegations = delegations;
        this.comptes = comptes;
    }

    /** Saga OUVERTURE : création unique du compte et invitation ; 201, ou 200 pour une étape rejouée. */
    @PostMapping("/comptes")
    public ResponseEntity<CompteSaga> ouvrir(@Valid @RequestBody OuvertureRequest r, @RequestHeader(name = CLE, required = false) String cle, Authentication auth) {
        CompteSaga c = comptes.ouvrir(ActeurCourant.de(auth), new NouveauCompte(r.identifiant(), r.prenom(), r.nom(), r.email(), r.telephone(), r.fonction(), r.institutionId(), r.profils()), cle);
        return ResponseEntity.status(c.rejouee() ? HttpStatus.OK : HttpStatus.CREATED).location(URI.create("/api/v1/interne/sandbox/comptes/" + c.id())).body(c);
    }

    @GetMapping("/comptes/{id}")
    public CompteSaga compte(@PathVariable String id) {
        return comptes.lire(id);
    }

    /** Saga MATRICE : seuls les profils SBX changent, les autres accès du compte sont conservés. */
    @PutMapping("/comptes/{id}/profils-sandbox")
    public CompteSaga matrice(@PathVariable String id, @Valid @RequestBody MatriceRequest r, @RequestHeader(name = CLE, required = false) String cle, Authentication auth) {
        return comptes.matrice(ActeurCourant.de(auth), id, r.profils(), r.motif(), cle);
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
