package cm.gimac.des.participant.web.rest.lot1;

import cm.gimac.des.participant.application.usecase.InstitutionInterneQueries;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Interface interne (jeton de service INTERNAL_PARTICIPANT_READ), jamais relayée par la passerelle. */
@RestController
@RequestMapping("/api/v1/interne/institutions")
public class InstitutionInterneResource {

    public record ServiceSouscrit(String code, String depuis) {}

    public record InstitutionInterne(
        UUID id,
        String nom,
        String statut,
        String categorie,
        String pays,
        String codeParticipant,
        List<ServiceSouscrit> servicesSouscrits
    ) {}

    private final InstitutionInterneQueries queries;

    public InstitutionInterneResource(InstitutionInterneQueries queries) {
        this.queries = queries;
    }

    @GetMapping("/{id}")
    public InstitutionInterne lire(@PathVariable UUID id) {
        return vue(queries.lire(id));
    }

    /** Institution d'un code membre (reprise des liaisons, D-107) : 404 PAR_INSTITUTION_NOT_FOUND si aucune. */
    @GetMapping(params = "codeParticipant")
    public InstitutionInterne parCode(@RequestParam String codeParticipant) {
        return vue(queries.parCode(codeParticipant));
    }

    private static InstitutionInterne vue(InstitutionInterneQueries.Vue v) {
        var i = v.institution();
        return new InstitutionInterne(
            i.getId(),
            i.getName(),
            i.getStatus().name(),
            i.getCategory() == null ? null : i.getCategory().name(),
            i.getCountry(),
            i.getParticipantCode(),
            v.servicesSouscrits().stream().map(s -> new ServiceSouscrit(s.getServiceCode(), s.getSubscribedOn() == null ? null : s.getSubscribedOn().toString())).toList()
        );
    }
}
