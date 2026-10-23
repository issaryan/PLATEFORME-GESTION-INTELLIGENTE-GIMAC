package cm.gimac.des.notification.web.rest.sandbox;

import cm.gimac.des.notification.application.port.in.RecevoirNotificationsSandboxUseCase;
import cm.gimac.des.notification.application.port.in.RecevoirNotificationsSandboxUseCase.ModeleVue;
import cm.gimac.des.notification.application.port.in.RecevoirNotificationsSandboxUseCase.Resolution;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Règles de notification de la Sandbox GIMAC (US-EXI-09, docs/lot1/sandbox-existant-adapte.md §11), exposées par la
 * passerelle sous /bff/console/notification/sandbox : catalogue des modèles et résolution à blanc d'une demande (NOT-05),
 * sans rien créer ni publier. Réservé à l'administration des habilitations de la sandbox (SBX_HAB_ADMINISTRATION).
 */
@RestController
@RequestMapping("/api/v1/sandbox")
public class NotificationsSandboxResource {

    static final String ADMINISTRATION = "hasAuthority('SBX_HAB_ADMINISTRATION')";

    /** Demande à résoudre à blanc : modèle, destinataires (comptes ou ROLE:…), code participant. */
    public record DemandeSimulee(String modele, List<String> destinataires, String codeParticipant) {}

    private final RecevoirNotificationsSandboxUseCase sandbox;

    public NotificationsSandboxResource(RecevoirNotificationsSandboxUseCase sandbox) {
        this.sandbox = sandbox;
    }

    @GetMapping("/modeles")
    @PreAuthorize(ADMINISTRATION)
    public List<ModeleVue> modeles() {
        return sandbox.modeles();
    }

    @PostMapping("/resolution")
    @PreAuthorize(ADMINISTRATION)
    public List<Resolution> resolution(@RequestBody DemandeSimulee d) {
        return sandbox.simuler(d.modele(), d.destinataires() == null ? List.of() : d.destinataires(), d.codeParticipant());
    }
}
