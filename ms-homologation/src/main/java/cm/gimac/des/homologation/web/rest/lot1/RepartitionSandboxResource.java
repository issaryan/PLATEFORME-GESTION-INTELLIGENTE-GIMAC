package cm.gimac.des.homologation.web.rest.lot1;

import cm.gimac.des.homologation.application.usecase.sandbox.RepartitionSandbox;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Répartition des exécutions d'une campagne relevée (D-121), en lecture seule, pour la vue graphique de l'onglet
 * sandbox du dossier : verdicts par cas et chronologie par jour (fuseau du GIMAC, annoncé dans la réponse). Servie à
 * l'application du département sous {@code /bff/des/homologation/liaisons/{id}/sandbox/...}, comme les résultats.
 */
@RestController
@RequestMapping("/api/v1/liaisons/{id}/sandbox/campagnes/{campagneId}/repartition")
public class RepartitionSandboxResource {

    private final RepartitionSandbox repartition;

    public RepartitionSandboxResource(RepartitionSandbox repartition) {
        this.repartition = repartition;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('HOM_LIAISON_READ')")
    public RepartitionSandbox.Repartition repartition(@PathVariable UUID id, @PathVariable String campagneId) {
        return repartition.repartition(id, campagneId);
    }
}
