package cm.gimac.des.homologation.web.rest.lot1;

import cm.gimac.des.homologation.application.usecase.sandbox.RepublicationEtatsLiaison;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Route d'exploitation interne (D-122), jeton de service portant INTERNAL_HOMOLOGATION_REPUBLISH : republie l'état
 * courant de chaque liaison (homologation.liaison-status-changed.v1, même version d'état) pour alimenter la projection
 * d'un consommateur de la Sandbox GIMAC. Jamais relayée par la passerelle (règle « interne » de l'OPA).
 */
@RestController
@RequestMapping("/api/v1/interne/sandbox/liaisons")
public class RepublicationSandboxResource {

    static final String PERMISSION = "INTERNAL_HOMOLOGATION_REPUBLISH";

    private final RepublicationEtatsLiaison republication;

    public RepublicationSandboxResource(RepublicationEtatsLiaison republication) {
        this.republication = republication;
    }

    @PostMapping("/republication")
    @PreAuthorize("hasAuthority('INTERNAL_HOMOLOGATION_REPUBLISH')")
    public RepublicationEtatsLiaison.Rapport republier(Authentication demandeur) {
        return republication.republier(demandeur.getName());
    }
}
