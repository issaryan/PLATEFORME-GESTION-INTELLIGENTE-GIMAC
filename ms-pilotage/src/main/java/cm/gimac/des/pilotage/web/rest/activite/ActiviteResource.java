package cm.gimac.des.pilotage.web.rest.activite;

import static cm.gimac.des.pilotage.web.rest.activite.Jetons.appelant;

import cm.gimac.des.pilotage.application.port.in.ActivitesUseCase;
import cm.gimac.des.pilotage.application.port.in.ActivitesUseCase.ActiviteSaisie;
import cm.gimac.des.pilotage.web.rest.activite.Vues.ActiviteRequete;
import cm.gimac.des.pilotage.web.rest.activite.Vues.ActiviteVue;
import cm.gimac.des.pilotage.web.rest.activite.Vues.StatutRequete;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Activités du jour (docs/lot1/mon-activite.md §2.2), PIL_ACTIVITY_OWN. */
@RestController
@RequestMapping("/api/v1/activites")
public class ActiviteResource {

    private final ActivitesUseCase activites;

    public ActiviteResource(ActivitesUseCase activites) {
        this.activites = activites;
    }

    @GetMapping
    @PreAuthorize(Jetons.ACTIVITE)
    public List<ActiviteVue> lister(
        @RequestParam(name = "du", required = false) String du,
        @RequestParam(name = "au", required = false) String au,
        @RequestParam(name = "collaborateur", required = false) String collaborateur,
        Authentication auth
    ) {
        return activites.activites(appelant(auth), du, au, collaborateur).stream().map(ActiviteVue::de).toList();
    }

    @PostMapping
    @PreAuthorize(Jetons.ACTIVITE)
    public ResponseEntity<ActiviteVue> creer(@RequestBody(required = false) ActiviteRequete requete, Authentication auth) {
        ActiviteVue vue = ActiviteVue.de(activites.creer(appelant(auth), saisie(requete)));
        return ResponseEntity.created(URI.create("/api/v1/activites/" + vue.id())).body(vue);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Jetons.ACTIVITE)
    public ActiviteVue modifier(@PathVariable UUID id, @RequestBody(required = false) ActiviteRequete requete, Authentication auth) {
        return ActiviteVue.de(activites.modifier(appelant(auth), id, saisie(requete)));
    }

    @PostMapping("/{id}/statut")
    @PreAuthorize(Jetons.ACTIVITE)
    public ActiviteVue statut(@PathVariable UUID id, @RequestBody(required = false) StatutRequete requete, Authentication auth) {
        return ActiviteVue.de(
            activites.changerStatut(appelant(auth), id, requete == null ? null : requete.statut(), requete == null ? null : requete.reporteeAu())
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Jetons.ACTIVITE)
    public ResponseEntity<Void> supprimer(@PathVariable UUID id, Authentication auth) {
        activites.supprimer(appelant(auth), id);
        return ResponseEntity.noContent().build();
    }

    private static ActiviteSaisie saisie(ActiviteRequete r) {
        return r == null ? null : new ActiviteSaisie(r.jour(), r.titre(), r.description(), r.dureeMinutes(), r.ordre(), r.tacheId());
    }
}
