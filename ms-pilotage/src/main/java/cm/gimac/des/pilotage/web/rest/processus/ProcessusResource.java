package cm.gimac.des.pilotage.web.rest.processus;

import static cm.gimac.des.pilotage.web.rest.activite.Jetons.appelant;

import cm.gimac.des.pilotage.application.port.in.InstancesProcessusUseCase;
import cm.gimac.des.pilotage.application.port.in.ProcessusUseCase;
import cm.gimac.des.pilotage.application.port.in.ProcessusUseCase.Controle;
import cm.gimac.des.pilotage.application.port.in.ProcessusUseCase.ProcessusSaisie;
import cm.gimac.des.pilotage.web.rest.activite.Jetons;
import cm.gimac.des.pilotage.web.rest.processus.VuesProcessus.ControleVue;
import cm.gimac.des.pilotage.web.rest.processus.VuesProcessus.InstanceVue;
import cm.gimac.des.pilotage.web.rest.processus.VuesProcessus.MotifRequete;
import cm.gimac.des.pilotage.web.rest.processus.VuesProcessus.ProcessusResumeVue;
import cm.gimac.des.pilotage.web.rest.processus.VuesProcessus.ProcessusVue;
import cm.gimac.des.pilotage.web.rest.processus.VuesProcessus.PublicationRequete;
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

/**
 * Définitions des processus d'unité (docs/lot1/processus-unite.md §3, passerelle /bff/des/pilotage/processus/**
 * vers /api/v1/processus/**), PIL_ACTIVITY_OWN sur chaque route ; le cas d'utilisation contrôle les droits fins
 * (PROCESSUS_DEFINIR couvrant l'unité ou IAM_PROFILE_EDIT ; lecture par l'unité ; 403 PIL_PROCESSUS_INTERDIT).
 * Le lancement (POST /processus/{id}/instances) rend l'instance créée.
 */
@RestController
@RequestMapping("/api/v1/processus")
public class ProcessusResource {

    private final ProcessusUseCase processus;
    private final InstancesProcessusUseCase instances;

    public ProcessusResource(ProcessusUseCase processus, InstancesProcessusUseCase instances) {
        this.processus = processus;
        this.instances = instances;
    }

    @GetMapping
    @PreAuthorize(Jetons.ACTIVITE)
    public List<ProcessusResumeVue> lister(
        @RequestParam(name = "unite", required = false) String unite,
        @RequestParam(name = "etat", required = false) String etat,
        @RequestParam(name = "q", required = false) String q,
        Authentication auth
    ) {
        return processus.lister(appelant(auth), unite, etat, q).stream().map(ProcessusResumeVue::de).toList();
    }

    @PostMapping
    @PreAuthorize(Jetons.ACTIVITE)
    public ResponseEntity<ProcessusVue> creer(@RequestBody(required = false) ProcessusSaisie saisie, Authentication auth) {
        ProcessusVue v = ProcessusVue.de(processus.creer(appelant(auth), saisie));
        return ResponseEntity.created(URI.create("/api/v1/processus/" + v.id())).body(v);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Jetons.ACTIVITE)
    public ProcessusVue lire(@PathVariable UUID id, Authentication auth) {
        return ProcessusVue.de(processus.lire(appelant(auth), id));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Jetons.ACTIVITE)
    public ProcessusVue modifier(@PathVariable UUID id, @RequestBody(required = false) ProcessusSaisie saisie, Authentication auth) {
        return ProcessusVue.de(processus.modifier(appelant(auth), id, saisie));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Jetons.ACTIVITE)
    public ResponseEntity<Void> supprimer(@PathVariable UUID id, Authentication auth) {
        processus.supprimer(appelant(auth), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/controle")
    @PreAuthorize(Jetons.ACTIVITE)
    public ControleVue controler(@PathVariable UUID id, Authentication auth) {
        Controle c = processus.controler(appelant(auth), id);
        return new ControleVue(c.publiable(), c.anomalies());
    }

    @PostMapping("/{id}/publication")
    @PreAuthorize(Jetons.ACTIVITE)
    public ProcessusVue publier(@PathVariable UUID id, @RequestBody(required = false) PublicationRequete r, Authentication auth) {
        return ProcessusVue.de(processus.publier(appelant(auth), id, r == null ? null : r.motif(), r != null && Boolean.TRUE.equals(r.leverAvertissements())));
    }

    @PostMapping("/{id}/nouvelle-version")
    @PreAuthorize(Jetons.ACTIVITE)
    public ResponseEntity<ProcessusVue> nouvelleVersion(@PathVariable UUID id, Authentication auth) {
        ProcessusVue v = ProcessusVue.de(processus.nouvelleVersion(appelant(auth), id));
        return ResponseEntity.created(URI.create("/api/v1/processus/" + v.id())).body(v);
    }

    @PostMapping("/{id}/retrait")
    @PreAuthorize(Jetons.ACTIVITE)
    public ProcessusVue retirer(@PathVariable UUID id, @RequestBody(required = false) MotifRequete r, Authentication auth) {
        return ProcessusVue.de(processus.retirer(appelant(auth), id, r == null ? null : r.motif()));
    }

    @PostMapping("/{id}/instances")
    @PreAuthorize(Jetons.ACTIVITE)
    public ResponseEntity<InstanceVue> lancer(@PathVariable UUID id, @RequestBody(required = false) InstancesProcessusUseCase.Lancement l, Authentication auth) {
        InstanceVue v = InstanceVue.de(instances.lancer(appelant(auth), id, l));
        return ResponseEntity.created(URI.create("/api/v1/processus/instances/" + v.id())).body(v);
    }
}
