package cm.gimac.des.iam.web.rest.lot1;

import cm.gimac.des.iam.application.usecase.AdministrationDelegueeService;
import cm.gimac.des.iam.application.usecase.AdministrationDelegueeService.Creation;
import cm.gimac.des.iam.application.usecase.AdministrationDelegueeService.NouveauCollaborateur;
import cm.gimac.des.iam.application.usecase.AdministrationDelegueeService.PageComptes;
import cm.gimac.des.iam.domain.compte.Compte;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administration déléguée des comptes d'une institution dans la sandbox (D-115, docs/lot1/sandbox-existant-adapte.md §4
 * US-EXI-03) : administrateur de l'institution (SBX_HAB_ADMINISTRATION), institution lue dans le jeton
 * (institution_id), jeton à deux facteurs (D-61). En-tête {@value #CLE} : clé d'idempotence d'une étape de saga de
 * SBX-ESPACE, rejouable sans double effet.
 */
@RestController
@RequestMapping("/api/v1/administration-deleguee")
@PreAuthorize("hasAuthority('SBX_HAB_ADMINISTRATION')")
public class AdministrationDelegueeResource {

    static final String CLE = "Idempotency-Key";

    public record CreationRequest(
        @NotBlank @Size(max = 50) String identifiant,
        @NotBlank @Size(max = 100) String prenom,
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Size(max = 255) String email,
        @Size(max = 30) String telephone,
        @Size(max = 120) String fonction,
        @NotNull Set<String> roles
    ) {}

    public record RolesRequest(@NotNull Set<String> roles, @NotBlank @Size(max = 500) String motif) {}

    public record EtatRequest(@NotNull Boolean actif, @NotBlank @Size(max = 500) String motif) {}

    private final AdministrationDelegueeService service;

    public AdministrationDelegueeResource(AdministrationDelegueeService service) {
        this.service = service;
    }

    private static String institution(Authentication auth) {
        return auth instanceof JwtAuthenticationToken jwt ? jwt.getToken().getClaimAsString("institution_id") : null;
    }

    @GetMapping("/roles")
    public Set<String> roles(Authentication auth) {
        cm.gimac.des.iam.domain.service.ReglesAdministrationDeleguee.institution(institution(auth));
        return service.roles();
    }

    @GetMapping("/comptes")
    public PageComptes comptes(
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "taille", defaultValue = "20") int taille,
        Authentication auth
    ) {
        return service.lister(institution(auth), page, taille);
    }

    @GetMapping("/comptes/{id}")
    public Compte compte(@PathVariable String id, Authentication auth) {
        return service.lire(institution(auth), id);
    }

    @PostMapping("/comptes")
    public ResponseEntity<Creation> creer(@Valid @RequestBody CreationRequest r, @RequestHeader(name = CLE, required = false) String cle, Authentication auth) {
        Creation c = service.creer(
            ActeurCourant.de(auth),
            institution(auth),
            new NouveauCollaborateur(r.identifiant().strip(), r.prenom().strip(), r.nom().strip(), r.email().strip(), r.telephone(), r.fonction(), r.roles()),
            cle
        );
        return ResponseEntity.status(c.rejouee() ? HttpStatus.OK : HttpStatus.CREATED)
            .location(URI.create("/api/v1/administration-deleguee/comptes/" + c.compte().id()))
            .body(c);
    }

    @PutMapping("/comptes/{id}/roles")
    public Compte roles(@PathVariable String id, @Valid @RequestBody RolesRequest r, @RequestHeader(name = CLE, required = false) String cle, Authentication auth) {
        return service.definirRoles(ActeurCourant.de(auth), institution(auth), id, r.roles(), r.motif(), cle);
    }

    /** Motif d'une demande de déblocage. */
    public record DeblocageRequest(@Size(max = 500) String motif) {}

    /** US-EXI-23 : déblocage réservé au GIMAC ; la demande de l'institution est refusée et journalisée (403). */
    @PostMapping("/comptes/{id}/deblocage")
    public Compte deblocage(@PathVariable String id, @RequestBody(required = false) DeblocageRequest r, Authentication auth) {
        return service.refuserDeblocage(ActeurCourant.de(auth), institution(auth), id, r == null ? null : r.motif());
    }

    @PutMapping("/comptes/{id}/etat")
    public Compte etat(@PathVariable String id, @Valid @RequestBody EtatRequest r, @RequestHeader(name = CLE, required = false) String cle, Authentication auth) {
        return service.definirActif(ActeurCourant.de(auth), institution(auth), id, r.actif(), r.motif(), cle);
    }
}
