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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * « Mon compte » (D-62, D-65), pour tout utilisateur authentifié à deux facteurs : première
 * configuration, coordonnées, mot de passe et code TOTP, clés de sécurité, sessions. Exposé sous
 * /bff/{des,portail,console}/iam/moi. Pendant le premier accès (jeton acr 1), seules GET /moi et les
 * routes de l'assistant (première configuration, mot de passe, second facteur) sont admises.
 */
@RestController
@RequestMapping("/api/v1/moi")
public class MonCompteResource {

    /** « avatarVersion » : version de la photo de profil, nulle sans photo (docs/lot1/mon-compte.md §1). */
    public record MoiVue(CompteVue compte, long clesSecurite, int sessions, List<String> etapesRestantes, String avatarVersion) {
        static MoiVue de(MonCompteService.MonCompte m) {
            return new MoiVue(CompteVue.de(m.compte()).avecAvatar(m.avatarVersion()), m.clesSecurite(), m.sessions(), m.etapesRestantes(), m.avatarVersion());
        }
    }

    /**
     * En-tête de la réponse de POST /moi/mot-de-passe quand les autres sessions doivent être fermées
     * (docs/lot1/mon-compte.md §2) : la passerelle, porteuse des sessions, ferme alors les autres
     * sessions du compte et retire l'en-tête avant de répondre au navigateur.
     */
    public static final String EN_TETE_FERMER_AUTRES_SESSIONS = "X-Gimac-Fermer-Autres-Sessions";

    public record PremiereConfigurationRequest(@NotBlank @Size(max = 20) String telephone, @NotBlank @Size(max = 5) String langue, @Size(max = 200) String fonction, boolean charteAcceptee) {}

    public record FicheRequest(@Size(max = 100) String prenom, @Size(max = 100) String nom, @Size(max = 20) String telephone, @Size(max = 200) String fonction, @Size(max = 5) String langue) {}

    public record ActionRequest(@NotNull ActionRequise action) {}

    public record EnrolementRequest(@NotBlank String challengeId, @NotBlank @Size(max = 100) String nom, @NotBlank String clientDataJSON, @NotBlank String attestationObject) {}

    /** Mots de passe : jamais dans une trace. « fermerAutresSessions » facultatif (mon-compte.md §2). */
    public record MotDePasseRequest(@NotNull @Size(max = 256) String actuel, @NotNull @Size(max = 256) String nouveau, Boolean fermerAutresSessions) {
        public MotDePasseRequest(String actuel, String nouveau) {
            this(actuel, nouveau, null);
        }

        public boolean fermer() {
            return Boolean.TRUE.equals(fermerAutresSessions);
        }

        @Override
        public String toString() {
            return "MotDePasseRequest[actuel=****, nouveau=****, fermerAutresSessions=" + fermer() + "]";
        }
    }

    /** Code saisi : jamais dans une trace. */
    public record TotpRequest(@NotNull UUID enrolementId, @NotBlank @Size(max = 10) String code, @Size(max = 100) String libelle) {
        @Override
        public String toString() {
            return "TotpRequest[enrolementId=" + enrolementId + ", code=****]";
        }
    }

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

    /** Changement du mot de passe (premier accès ou « Mon compte ») ; retire l'action MOT_DE_PASSE. */
    @PostMapping("/mot-de-passe")
    public ResponseEntity<Void> changerMotDePasse(@Valid @RequestBody MotDePasseRequest r, Authentication auth) {
        service.changerMotDePasse(ActeurCourant.de(auth), r.actuel(), r.nouveau(), r.fermer(), sessionCourante(auth));
        ResponseEntity.HeadersBuilder<?> reponse = ResponseEntity.noContent();
        if (r.fermer()) {
            reponse.header(EN_TETE_FERMER_AUTRES_SESSIONS, "1");
        }
        return reponse.build();
    }

    /** Session du fournisseur d'identité qui a émis le jeton (claim sid de Keycloak). */
    static String sessionCourante(Authentication auth) {
        return auth instanceof JwtAuthenticationToken jwt ? jwt.getToken().getClaimAsString("sid") : null;
    }

    /** Début de la configuration du code TOTP : secret (base 32) et URI otpauth du code QR, 10 min. */
    @PostMapping("/second-facteur/totp/enrolement")
    public MonCompteService.Enrolement enrolementTotp(Authentication auth) {
        return service.enrolementTotp(ActeurCourant.de(auth));
    }

    /** Fin de la configuration : code vérifié puis enregistré ; FIDO2 exigé pour remplacer un code (clé enrôlée). */
    @PostMapping("/second-facteur/totp")
    public ResponseEntity<Void> confirmerTotp(@Valid @RequestBody TotpRequest r, @RequestHeader(name = ComptesResource.REAUTH, required = false) String assertion, Authentication auth) {
        service.confirmerTotp(ActeurCourant.de(auth), r.enrolementId(), r.code(), r.libelle(), assertion);
        return ResponseEntity.noContent().build();
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
