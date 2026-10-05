package cm.gimac.des.pilotage.web.rest.activite;

import static cm.gimac.des.pilotage.web.rest.activite.Jetons.appelant;

import cm.gimac.des.pilotage.application.port.in.TachesUseCase;
import cm.gimac.des.pilotage.application.port.in.TachesUseCase.TacheSaisie;
import cm.gimac.des.pilotage.web.rest.activite.Vues.PageVue;
import cm.gimac.des.pilotage.web.rest.activite.Vues.TacheRequete;
import cm.gimac.des.pilotage.web.rest.activite.Vues.TacheVue;
import cm.gimac.des.pilotage.web.rest.activite.Vues.TransitionRequete;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tâches (docs/lot1/mon-activite.md §2.3), PIL_ACTIVITY_OWN ; vue equipe : PIL_TEAM_READ ; confier à
 * un membre de son équipe : PIL_TASK_ASSIGN (contrôlés par le cas d'utilisation, 403 PIL_HORS_EQUIPE
 * ou PIL_ACTION_RESERVEE).
 */
@RestController
@RequestMapping("/api/v1/taches")
public class TacheResource {

    private final TachesUseCase taches;

    public TacheResource(TachesUseCase taches) {
        this.taches = taches;
    }

    @GetMapping
    @PreAuthorize(Jetons.ACTIVITE)
    public PageVue<TacheVue> lister(
        @RequestParam(name = "vue", required = false) String vue,
        @RequestParam(name = "etat", required = false) String etat,
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "taille", required = false) Integer taille,
        Authentication auth
    ) {
        return PageVue.de(taches.taches(appelant(auth), vue, etat, page, taille), TacheVue::de);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Jetons.ACTIVITE)
    public TacheVue detail(@PathVariable UUID id, Authentication auth) {
        return TacheVue.de(taches.tache(appelant(auth), id));
    }

    @PostMapping
    @PreAuthorize(Jetons.ACTIVITE)
    public ResponseEntity<TacheVue> creer(@RequestBody(required = false) TacheRequete requete, Authentication auth) {
        TacheVue vue = TacheVue.de(taches.creer(appelant(auth), saisie(requete)));
        return ResponseEntity.created(URI.create("/api/v1/taches/" + vue.id())).body(vue);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Jetons.ACTIVITE)
    public TacheVue modifier(@PathVariable UUID id, @RequestBody(required = false) TacheRequete requete, Authentication auth) {
        return TacheVue.de(taches.modifier(appelant(auth), id, saisie(requete)));
    }

    @PostMapping("/{id}/transition")
    @PreAuthorize(Jetons.ACTIVITE)
    public TacheVue transition(@PathVariable UUID id, @RequestBody(required = false) TransitionRequete requete, Authentication auth) {
        return TacheVue.de(taches.transiter(appelant(auth), id, requete == null ? null : requete.etat(), requete == null ? null : requete.motif()));
    }

    private static TacheSaisie saisie(TacheRequete r) {
        return r == null ? null : new TacheSaisie(r.titre(), r.description(), r.echeance(), r.priorite(), r.responsableId());
    }
}
