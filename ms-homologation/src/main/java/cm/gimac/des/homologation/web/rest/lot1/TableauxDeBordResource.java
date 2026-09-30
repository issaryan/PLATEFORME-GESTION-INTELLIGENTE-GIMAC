package cm.gimac.des.homologation.web.rest.lot1;

import cm.gimac.des.homologation.application.usecase.ParcoursLectures;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Personne;
import cm.gimac.des.homologation.domain.parcours.Vues;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tableaux de bord par fonction (D-58), pilotage (CR §7.3, annexe B) et intervenants d'une fonction,
 * exposés sous /bff/des/homologation. Chaque acteur ne reçoit que les fonctions que porte son jeton.
 */
@RestController
@RequestMapping("/api/v1")
public class TableauxDeBordResource {

    private final ParcoursLectures lectures;

    public TableauxDeBordResource(ParcoursLectures lectures) {
        this.lectures = lectures;
    }

    @GetMapping("/tableaux-de-bord/acteur")
    @PreAuthorize("hasAuthority('HOM_DASHBOARD_READ')")
    public Vues.TableauActeur acteur(@RequestParam(name = "fonctions", required = false) String fonctions, Authentication auth) {
        return lectures.tableauActeur(fonctions(fonctions), ActeurCourant.de(auth));
    }

    @GetMapping("/tableaux-de-bord/pilotage")
    @PreAuthorize("hasAuthority('HOM_DASHBOARD_READ')")
    public Vues.Pilotage pilotage() {
        return lectures.pilotage();
    }

    @GetMapping("/intervenants")
    @PreAuthorize("hasAnyAuthority('HOM_LIAISON_READ', 'HOM_OPERATION_ASSIGN', 'HOM_STEP_MANAGE')")
    public List<Personne> intervenants(@RequestParam(name = "fonction") Fonction fonction) {
        return lectures.intervenants(fonction);
    }

    static List<Fonction> fonctions(String liste) {
        if (liste == null || liste.isBlank()) {
            return List.of();
        }
        try {
            return Arrays.stream(liste.split(",")).map(String::trim).filter(s -> !s.isEmpty()).map(Fonction::valueOf).toList();
        } catch (IllegalArgumentException e) {
            throw HomologationException.unprocessable("HOM_FUNCTION_UNKNOWN", "Fonction inconnue.");
        }
    }
}
