package cm.gimac.des.homologation.web.rest.lot1;

import cm.gimac.des.homologation.application.usecase.sandbox.ResultatsSandbox;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Résultats de la sandbox d'une liaison, en lecture seule (D-118 point 1, US-EXI-14, US-EXI-17), servis à
 * l'application du département sous {@code /bff/des/homologation/liaisons/{id}/sandbox} : aucune route d'écriture, et
 * aucune route de l'application du département vers un service de la sandbox.
 */
@RestController
@RequestMapping("/api/v1/liaisons/{id}/sandbox")
public class ResultatsSandboxResource {

    private final ResultatsSandbox resultats;

    public ResultatsSandboxResource(ResultatsSandbox resultats) {
        this.resultats = resultats;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('HOM_LIAISON_READ')")
    public ResultatsSandbox.Resultats resultats(@PathVariable UUID id) {
        return resultats.resultats(id);
    }

    @GetMapping("/campagnes/{campagneId}/executions")
    @PreAuthorize("hasAuthority('HOM_LIAISON_READ')")
    public ResultatsSandbox.PageExecutions executions(
        @PathVariable UUID id,
        @PathVariable String campagneId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int taille
    ) {
        return resultats.executions(id, campagneId, page, taille);
    }
}
