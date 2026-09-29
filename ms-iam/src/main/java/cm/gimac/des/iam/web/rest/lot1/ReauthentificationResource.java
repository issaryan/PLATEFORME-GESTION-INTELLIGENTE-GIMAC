package cm.gimac.des.iam.web.rest.lot1;

import cm.gimac.des.iam.application.usecase.ReauthentificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Réauthentification forte FIDO2 (IAM-07). Le défi est demandé par l'interface
 * (/bff/{couche}/iam/reauthentification/defi) ; l'assertion est vérifiée par le service qui porte
 * la décision, via la route interne, au nom de l'utilisateur et pour la finalité annoncée.
 */
@RestController
@RequestMapping("/api/v1")
public class ReauthentificationResource {

    public record DefiRequest(@NotBlank @Size(max = 60) String purpose) {}

    public record VerificationRequest(@NotBlank String assertion, @NotBlank String sujet, @NotBlank @Size(max = 60) String finalite) {}

    public record Verdict(boolean valide) {}

    private final ReauthentificationService service;

    public ReauthentificationResource(ReauthentificationService service) {
        this.service = service;
    }

    @PostMapping("/reauthentification/defi")
    public ReauthentificationService.Defi defi(@Valid @RequestBody DefiRequest r, Authentication auth) {
        return service.defi(ActeurCourant.de(auth), r.purpose());
    }

    /** Route interne, jeton de service (INTERNAL_IAM_READ) : l'assertion est consommée, jamais rejouable. */
    @PostMapping("/interne/reauthentifications/verification")
    public Verdict verifier(@Valid @RequestBody VerificationRequest r) {
        return new Verdict(service.verifier(r.assertion(), r.sujet(), r.finalite()));
    }
}
