package cm.gimac.des.homologation.web.rest.lot1;

import cm.gimac.des.homologation.application.usecase.ParcoursLectures;
import cm.gimac.des.homologation.domain.parcours.Vues;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Interface interne (docs/lot1/parcours-backend.md §2), jeton de service de MS-CERTIFICATION :
 * contexte d'une liaison pour établir le procès-verbal (100 % exigés, constat fait), déterminer les
 * attestations exigées et cloisonner le portail. Non exposée par la passerelle.
 */
@RestController
@RequestMapping("/api/v1/interne/liaisons")
public class ContexteCertificationResource {

    private final ParcoursLectures lectures;

    public ContexteCertificationResource(ParcoursLectures lectures) {
        this.lectures = lectures;
    }

    @GetMapping("/{id}/contexte-certification")
    @PreAuthorize("hasAuthority('INTERNAL_HOMOLOGATION_READ')")
    public Vues.ContexteCertification contexte(@PathVariable UUID id) {
        return lectures.contexteCertification(id);
    }
}
