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

    /**
     * Fonction du catalogue (D-91) portée par l'appelant : {@code profils} du jeton qui la lui confèrent (une fonction
     * collective, comme les chefs de département, découle d'une autre fonction et n'en a aucun).
     */
    public record FonctionAppelant(String code, String libelle, boolean externe, List<String> profils) {}

    /**
     * Fonctions de l'appelant (D-94 §3), déduites du catalogue des fonctions et des profils de son jeton : l'interface
     * n'a plus de table profil vers fonction.
     */
    @GetMapping("/moi/fonctions")
    @PreAuthorize("isAuthenticated()")
    public List<FonctionAppelant> mesFonctions(Authentication auth) {
        var autorites = ActeurCourant.de(auth).permissions();
        return cm.gimac.des.homologation.domain.service.VisibiliteReferentiel.fonctions(autorites)
            .stream()
            .sorted()
            .map(f -> new FonctionAppelant(f.name(), f.libelle(), f.externe(), f.profils().stream().filter(autorites::contains).toList()))
            .toList();
    }

    static List<Fonction> fonctions(String liste) {
        if (liste == null || liste.isBlank()) {
            return List.of();
        }
        return Arrays.stream(liste.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(s -> Fonction.duCatalogue(s).orElseThrow(() -> HomologationException.unprocessable("HOM_FUNCTION_UNKNOWN", "Fonction inconnue.")))
            .toList();
    }
}
