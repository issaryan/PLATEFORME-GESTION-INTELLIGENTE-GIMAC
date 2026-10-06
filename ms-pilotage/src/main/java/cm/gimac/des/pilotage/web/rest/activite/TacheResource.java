package cm.gimac.des.pilotage.web.rest.activite;

import static cm.gimac.des.pilotage.web.rest.activite.Jetons.appelant;

import cm.gimac.des.pilotage.application.port.in.TachesUseCase;
import cm.gimac.des.pilotage.application.port.in.TachesUseCase.TacheSaisie;
import cm.gimac.des.pilotage.web.rest.activite.Vues.AttributionRequete;
import cm.gimac.des.pilotage.web.rest.activite.Vues.GroupeVue;
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
 * Tâches (docs/lot1/mon-activite.md §2.3 ; tâches confiées, docs/lot1/pouvoirs-hierarchiques.md §6),
 * PIL_ACTIVITY_OWN sur chaque route ; le cas d'utilisation contrôle la matrice des pouvoirs (403
 * PIL_HORS_EQUIPE ou PIL_ACTION_RESERVEE) : confier, réaffecter, attribuer (PIL_TASK_ASSIGN et
 * TACHE_CONFIER), vue equipe et relance (PIL_TEAM_READ et TACHE_SUIVRE, ou créateur pour la relance).
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

    @GetMapping("/groupes/{groupeId}")
    @PreAuthorize(Jetons.ACTIVITE)
    public GroupeVue groupe(@PathVariable UUID groupeId, Authentication auth) {
        return GroupeVue.de(taches.groupe(appelant(auth), groupeId));
    }

    /** 201 : une tâche (TacheVue), ou, avec {@code responsables}, l'envoi à plusieurs personnes (GroupeVue). */
    @PostMapping
    @PreAuthorize(Jetons.ACTIVITE)
    public ResponseEntity<Object> creer(@RequestBody(required = false) TacheRequete requete, Authentication auth) {
        if (requete != null && requete.responsables() != null) {
            GroupeVue groupe = GroupeVue.de(taches.creerPourPlusieurs(appelant(auth), saisie(requete)));
            return ResponseEntity.created(URI.create("/api/v1/taches/groupes/" + groupe.groupeId())).body(groupe);
        }
        TacheVue vue = TacheVue.de(taches.creer(appelant(auth), saisie(requete)));
        return ResponseEntity.created(URI.create("/api/v1/taches/" + vue.id())).body(vue);
    }

    @PostMapping("/{id}/prise")
    @PreAuthorize(Jetons.ACTIVITE)
    public TacheVue prise(@PathVariable UUID id, Authentication auth) {
        return TacheVue.de(taches.prendre(appelant(auth), id));
    }

    @PostMapping("/{id}/attribution")
    @PreAuthorize(Jetons.ACTIVITE)
    public TacheVue attribution(@PathVariable UUID id, @RequestBody(required = false) AttributionRequete requete, Authentication auth) {
        return TacheVue.de(taches.attribuer(appelant(auth), id, requete == null ? null : requete.responsableId()));
    }

    @PostMapping("/{id}/relance")
    @PreAuthorize(Jetons.ACTIVITE)
    public TacheVue relance(@PathVariable UUID id, Authentication auth) {
        return TacheVue.de(taches.relancer(appelant(auth), id));
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
        return r == null ? null : new TacheSaisie(r.titre(), r.description(), r.echeance(), r.priorite(), r.responsableId(), r.responsables(), r.uniteCible());
    }
}
