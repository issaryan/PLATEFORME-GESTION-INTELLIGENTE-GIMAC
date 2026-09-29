package cm.gimac.des.iam.web.rest.lot1;

import cm.gimac.des.iam.application.usecase.MonCompteService;
import cm.gimac.des.iam.application.usecase.ReauthentificationService;
import cm.gimac.des.iam.domain.compte.ActionRequise;
import cm.gimac.des.iam.domain.compte.SessionCompte;
import cm.gimac.des.iam.web.rest.lot1.ComptesResource.CompteVue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * « Mon compte » (D-62), pour tout utilisateur authentifié à deux facteurs : première
 * configuration, coordonnées, mot de passe et second facteur, clés de sécurité, sessions. Exposé
 * sous /bff/{des,portail,console}/iam/moi.
 */
@RestController
@RequestMapping("/api/v1/moi")
public class MonCompteResource {

    public record MoiVue(CompteVue compte, long clesSecurite, int sessions, List<String> etapesRestantes) {
        static MoiVue de(MonCompteService.MonCompte m) {
            return new MoiVue(CompteVue.de(m.compte()), m.clesSecurite(), m.sessions(), m.etapesRestantes());
        }
    }

    public record PremiereConfigurationRequest(@NotBlank @Size(max = 20) String telephone, @NotBlank @Size(max = 5) String langue, @Size(max = 200) String fonction, boolean charteAcceptee) {}

    public record FicheRequest(@Size(max = 100) String prenom, @Size(max = 100) String nom, @Size(max = 20) String telephone, @Size(max = 200) String fonction, @Size(max = 5) String langue) {}

    public record ActionRequest(@NotNull ActionRequise action) {}

    public record EnrolementRequest(@NotBlank String challengeId, @NotBlank @Size(max = 100) String nom, @NotBlank String clientDataJSON, @NotBlank String attestationObject) {}

    private final MonCompteService service;
    private final ReauthentificationService reauth;

    public MonCompteResource(MonCompteService service, ReauthentificationService reauth) {
        this.service = service;
        this.reauth = reauth;
    }

    @GetMapping
    public MoiVue moi(Authentication auth) {
        return MoiVue.de(service.moi(ActeurCourant.de(auth)));
    }

    @PostMapping("/premiere-configuration")
    public MoiVue premiereConfiguration(@Valid @RequestBody PremiereConfigurationRequest r, Authentication auth) {
        return MoiVue.de(service.premiereConfiguration(ActeurCourant.de(auth), r.telephone(), r.langue(), r.fonction(), r.charteAcceptee()));
    }

    @PutMapping
    public MoiVue modifier(@Valid @RequestBody FicheRequest r, Authentication auth) {
        return MoiVue.de(service.modifier(ActeurCourant.de(auth), r.prenom(), r.nom(), r.telephone(), r.fonction(), r.langue()));
    }

    /** Changement de mot de passe ou de second facteur : l'interface reconnecte ensuite l'utilisateur. */
    @PostMapping("/actions")
    public MoiVue demander(@Valid @RequestBody ActionRequest r, Authentication auth) {
        return MoiVue.de(service.demander(ActeurCourant.de(auth), r.action()));
    }

    @GetMapping("/sessions")
    public List<SessionCompte> sessions(Authentication auth) {
        return service.sessions(ActeurCourant.de(auth));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> fermerSession(@PathVariable String sessionId, Authentication auth) {
        service.fermerSession(ActeurCourant.de(auth), sessionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/sessions")
    public ResponseEntity<Void> fermerSessions(Authentication auth) {
        service.fermerToutesSessions(ActeurCourant.de(auth));
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ clés de sécurité FIDO2

    @GetMapping("/cles-securite")
    public List<ReauthentificationService.Cle> cles(Authentication auth) {
        return reauth.cles(ActeurCourant.de(auth));
    }

    @PostMapping("/cles-securite/defi")
    public ReauthentificationService.OptionsEnrolement defiEnrolement(Authentication auth) {
        return reauth.defiEnrolement(ActeurCourant.de(auth));
    }

    @PostMapping("/cles-securite")
    public ReauthentificationService.Cle enroler(@Valid @RequestBody EnrolementRequest r, Authentication auth) {
        return reauth.enroler(ActeurCourant.de(auth), r.challengeId(), r.nom(), r.clientDataJSON(), r.attestationObject());
    }

    @DeleteMapping("/cles-securite/{id}")
    public ResponseEntity<Void> supprimerCle(@PathVariable UUID id, Authentication auth) {
        reauth.supprimerCle(ActeurCourant.de(auth), id);
        return ResponseEntity.noContent().build();
    }
}
