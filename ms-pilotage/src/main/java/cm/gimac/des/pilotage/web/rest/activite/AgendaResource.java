package cm.gimac.des.pilotage.web.rest.activite;

import static cm.gimac.des.pilotage.web.rest.activite.Jetons.appelant;

import cm.gimac.des.pilotage.application.port.in.AgendaUseCase;
import cm.gimac.des.pilotage.application.port.in.AgendaUseCase.EvenementSaisi;
import cm.gimac.des.pilotage.web.rest.activite.Vues.EvenementRequete;
import cm.gimac.des.pilotage.web.rest.activite.Vues.EvenementVue;
import cm.gimac.des.pilotage.web.rest.activite.Vues.ReponseRequete;
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

/** Agenda (docs/lot1/mon-activite.md §2.1), PIL_ACTIVITY_OWN ; agenda d'un membre de l'équipe avec PIL_TEAM_READ. */
@RestController
@RequestMapping("/api/v1/agenda")
public class AgendaResource {

    private final AgendaUseCase agenda;

    public AgendaResource(AgendaUseCase agenda) {
        this.agenda = agenda;
    }

    @GetMapping
    @PreAuthorize(Jetons.ACTIVITE)
    public List<EvenementVue> lister(
        @RequestParam(name = "du", required = false) String du,
        @RequestParam(name = "au", required = false) String au,
        @RequestParam(name = "collaborateur", required = false) String collaborateur,
        Authentication auth
    ) {
        return agenda.agenda(appelant(auth), du, au, collaborateur).stream().map(EvenementVue::de).toList();
    }

    @PostMapping
    @PreAuthorize(Jetons.ACTIVITE)
    public ResponseEntity<EvenementVue> creer(@RequestBody(required = false) EvenementRequete requete, Authentication auth) {
        EvenementVue vue = EvenementVue.de(agenda.creer(appelant(auth), saisie(requete)));
        return ResponseEntity.created(URI.create("/api/v1/agenda/" + vue.id())).body(vue);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Jetons.ACTIVITE)
    public EvenementVue modifier(@PathVariable UUID id, @RequestBody(required = false) EvenementRequete requete, Authentication auth) {
        return EvenementVue.de(agenda.modifier(appelant(auth), id, saisie(requete)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Jetons.ACTIVITE)
    public ResponseEntity<Void> supprimer(@PathVariable UUID id, Authentication auth) {
        agenda.supprimer(appelant(auth), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reponse")
    @PreAuthorize(Jetons.ACTIVITE)
    public EvenementVue repondre(@PathVariable UUID id, @RequestBody(required = false) ReponseRequete requete, Authentication auth) {
        return EvenementVue.de(agenda.repondre(appelant(auth), id, requete == null ? null : requete.reponse()));
    }

    private static EvenementSaisi saisie(EvenementRequete r) {
        return r == null
            ? null
            : new EvenementSaisi(r.titre(), r.description(), r.lieu(), r.debut(), r.fin(), r.journeeEntiere(), r.categorie(), r.participants());
    }
}
