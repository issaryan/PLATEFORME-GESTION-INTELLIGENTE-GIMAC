package cm.gimac.des.iam.web.rest.lot1;

import cm.gimac.des.iam.application.usecase.FonctionnalitesService;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.fonctionnalite.Fonctionnalite;
import cm.gimac.des.iam.infrastructure.security.CorrelationContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catalogue des fonctionnalités servi et configurable (D-94, docs/lot1/ecrans-par-configuration.md §2), sous
 * /api/v1/configuration/fonctionnalites (passerelle : /bff/{des,console}/iam/configuration/fonctionnalites).
 *
 * <ul>
 *   <li>GET ?application=des|console|portail : tout compte interne authentifié (jeton utilisateur sans
 *       institution, 403 IAM_COMPTE_INTERNE_EXIGE sinon), pour construire sa navigation.</li>
 *   <li>PUT /{cle} : IAM_PROFILE_EDIT, motif obligatoire, réauthentification forte FIDO2 (en-tête
 *       X-Reauth-Assertion) comme les autres actes de configuration ; journal CONFIGURATION_FONCTIONNALITE_MODIFIEE.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/configuration/fonctionnalites")
public class FonctionnalitesResource {

    /** Corps du PUT : la fonctionnalité (clé facultative, égale au chemin) et le motif. */
    public record ModificationRequest(
        @Size(max = 60) String cle,
        @NotBlank @Size(max = 20) String application,
        @NotBlank @Size(max = 40) String module,
        @NotBlank @Size(max = 200) String libelle,
        @Size(max = 40) String groupe,
        int ordre,
        @NotBlank @Size(max = 40) String icone,
        boolean visible,
        @NotNull @Valid Fonctionnalite.Predicat permissions,
        @Size(max = 20) List<Fonctionnalite.Widget> widgets,
        @Size(max = 60) List<Fonctionnalite.Droit> droits,
        @NotBlank @Size(max = 1000) String motif
    ) {
        Fonctionnalite fonctionnalite() {
            return new Fonctionnalite(cle, application, module, libelle, groupe, ordre, icone, visible, permissions, widgets, droits, null, null);
        }
    }

    private final FonctionnalitesService service;
    private final ReauthAdministration reauth;

    public FonctionnalitesResource(FonctionnalitesService service, ReauthAdministration reauth) {
        this.service = service;
        this.reauth = reauth;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<Fonctionnalite> lister(@RequestParam(name = "application", required = false) String application, Authentication auth) {
        ActeurCourant.de(auth);
        if (auth instanceof JwtAuthenticationToken jwt) {
            String institution = jwt.getToken().getClaimAsString(CorrelationContext.INSTITUTION_CLAIM);
            if (institution != null && !institution.isBlank()) {
                throw IamException.interdit("IAM_COMPTE_INTERNE_EXIGE", "Catalogue des fonctionnalités réservé aux comptes internes");
            }
        }
        return service.lister(application);
    }

    @PutMapping("/{cle}")
    @PreAuthorize("hasAuthority('IAM_PROFILE_EDIT')")
    public Fonctionnalite modifier(
        @PathVariable String cle,
        @Valid @RequestBody ModificationRequest r,
        @RequestHeader(name = ComptesResource.REAUTH, required = false) String assertion,
        Authentication auth
    ) {
        Acteur a = ActeurCourant.de(auth);
        reauth.exiger(a, assertion);
        return service.modifier(a, cle, r.fonctionnalite(), r.motif());
    }
}
