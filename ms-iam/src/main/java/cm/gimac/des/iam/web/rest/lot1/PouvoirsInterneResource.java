package cm.gimac.des.iam.web.rest.lot1;

import cm.gimac.des.iam.application.usecase.PouvoirsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pouvoirs d'un compte pour les autres services (D-81, docs/lot1/pouvoirs-hierarchiques.md §3 et §6),
 * jeton de service INTERNAL_IAM_READ (hors passerelle) : MS-PILOTAGE vérifie TACHE_CONFIER, TACHE_SUIVRE,
 * ACTIVITE_LIRE et RAPPORT_VISER (niveau 1 : direct ; 1 et plus : équipe ; 0 : membre d'une unité dirigée).
 */
@RestController
@RequestMapping("/api/v1/interne/pouvoirs")
public class PouvoirsInterneResource {

    private final PouvoirsService service;

    public PouvoirsInterneResource(PouvoirsService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public PouvoirsService.VuePouvoirs pouvoirs(@PathVariable String id) {
        return service.vue(id);
    }
}
