package cm.gimac.des.pilotage.web.rest.processus;

import static cm.gimac.des.pilotage.web.rest.activite.Jetons.appelant;

import cm.gimac.des.pilotage.application.port.in.InstancesProcessusUseCase;
import cm.gimac.des.pilotage.application.port.in.ProcessusUseCase;
import cm.gimac.des.pilotage.web.rest.activite.Jetons;
import cm.gimac.des.pilotage.web.rest.processus.VuesProcessus.ChampsRequete;
import cm.gimac.des.pilotage.web.rest.processus.VuesProcessus.InstanceResumeVue;
import cm.gimac.des.pilotage.web.rest.processus.VuesProcessus.InstanceVue;
import cm.gimac.des.pilotage.web.rest.processus.VuesProcessus.LivrableRequete;
import cm.gimac.des.pilotage.web.rest.processus.VuesProcessus.MotifRequete;
import cm.gimac.des.pilotage.web.rest.processus.VuesProcessus.PageVue;
import cm.gimac.des.pilotage.web.rest.processus.VuesProcessus.ReaffectationRequete;
import cm.gimac.des.pilotage.web.rest.processus.VuesProcessus.SignatureRequete;
import cm.gimac.des.pilotage.web.rest.processus.VuesProcessus.ValidationRequete;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Instances des processus d'unité (docs/lot1/processus-unite.md §2, §3), sous /api/v1/processus/instances,
 * PIL_ACTIVITY_OWN sur chaque route ; droits fins dans le cas d'utilisation (exécutant, signataire, validateur,
 * chef de l'unité). La signature exige l'en-tête X-Reauth-Assertion (FIDO2, finalité SIGNATURE_PROCESSUS).
 */
@RestController
@RequestMapping("/api/v1/processus/instances")
public class InstanceProcessusResource {

    static final String REAUTH = "X-Reauth-Assertion";

    private final InstancesProcessusUseCase instances;

    public InstanceProcessusResource(InstancesProcessusUseCase instances) {
        this.instances = instances;
    }

    @GetMapping
    @PreAuthorize(Jetons.ACTIVITE)
    public PageVue<InstanceResumeVue> lister(
        @RequestParam(name = "vue", required = false) String vue,
        @RequestParam(name = "etat", required = false) String etat,
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "taille", required = false) Integer taille,
        Authentication auth
    ) {
        return PageVue.de(instances.instances(appelant(auth), vue, etat, page, taille), InstanceResumeVue::de);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Jetons.ACTIVITE)
    public InstanceVue lire(@PathVariable UUID id, Authentication auth) {
        return InstanceVue.de(instances.instance(appelant(auth), id));
    }

    /**
     * QUO-04 : pièce du formulaire de lancement ou livrable déposé, relayé en flux depuis MS-DOCUMENT (jamais chargé en
     * entier) ; lisible de qui lit l'instance (initiateur, intervenants, porteurs d'une étape à prendre, unité).
     */
    @GetMapping("/{id}/documents/{documentId}")
    @PreAuthorize(Jetons.ACTIVITE)
    public ResponseEntity<Resource> document(@PathVariable UUID id, @PathVariable String documentId, Authentication auth) {
        InstancesProcessusUseCase.DocumentInstance d = instances.document(appelant(auth), id, documentId);
        MediaType type;
        try {
            type = MediaType.parseMediaType(d.typeMime() == null ? "application/octet-stream" : d.typeMime());
        } catch (InvalidMediaTypeException e) {
            type = MediaType.APPLICATION_OCTET_STREAM;
        }
        String nom = d.nom() == null || d.nom().isBlank() ? "document" : d.nom();
        ResponseEntity.BodyBuilder r = ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nom, StandardCharsets.UTF_8).build().toString())
            .header("X-Content-Type-Options", "nosniff")
            .contentType(type);
        if (d.taille() >= 0) {
            r = r.contentLength(d.taille());
        }
        return r.body(new InputStreamResource(d.flux()));
    }

    @PostMapping("/{id}/etapes/{code}/livrables")
    @PreAuthorize(Jetons.ACTIVITE)
    public InstanceVue deposer(@PathVariable UUID id, @PathVariable String code, @RequestBody(required = false) LivrableRequete r, Authentication auth) {
        return InstanceVue.de(instances.deposer(appelant(auth), id, code, r == null ? null : r.livrable(), r == null ? null : r.documentId()));
    }

    @PutMapping("/{id}/etapes/{code}/champs")
    @PreAuthorize(Jetons.ACTIVITE)
    public InstanceVue renseigner(@PathVariable UUID id, @PathVariable String code, @RequestBody(required = false) ChampsRequete r, Authentication auth) {
        return InstanceVue.de(instances.renseigner(appelant(auth), id, code, r == null ? null : r.valeurs()));
    }

    @PostMapping("/{id}/etapes/{code}/signature")
    @PreAuthorize(Jetons.ACTIVITE)
    public InstanceVue signer(
        @PathVariable UUID id,
        @PathVariable String code,
        @RequestBody(required = false) SignatureRequete r,
        @RequestHeader(name = REAUTH, required = false) String assertion,
        Authentication auth
    ) {
        return InstanceVue.de(instances.signer(appelant(auth), id, code, r == null ? null : r.signature(), assertion));
    }

    @PostMapping("/{id}/etapes/{code}/fin")
    @PreAuthorize(Jetons.ACTIVITE)
    public InstanceVue terminer(@PathVariable UUID id, @PathVariable String code, Authentication auth) {
        return InstanceVue.de(instances.terminer(appelant(auth), id, code));
    }

    @PostMapping("/{id}/etapes/{code}/validation")
    @PreAuthorize(Jetons.ACTIVITE)
    public InstanceVue valider(@PathVariable UUID id, @PathVariable String code, @RequestBody(required = false) ValidationRequete r, Authentication auth) {
        return InstanceVue.de(instances.valider(appelant(auth), id, code, r == null ? null : r.decision(), r == null ? null : r.motif()));
    }

    @PostMapping("/{id}/etapes/{code}/reaffectation")
    @PreAuthorize(Jetons.ACTIVITE)
    public InstanceVue reaffecter(@PathVariable UUID id, @PathVariable String code, @RequestBody(required = false) ReaffectationRequete r, Authentication auth) {
        return InstanceVue.de(instances.reaffecter(appelant(auth), id, code, r == null ? null : r.personneId(), r == null ? null : r.motif()));
    }

    /** Candidats à l'exécution d'une étape en cours (réaffectation) : dans l'unité, puis ailleurs dans le GIMAC. */
    @GetMapping("/{id}/etapes/{code}/candidats")
    @PreAuthorize(Jetons.ACTIVITE)
    public ProcessusUseCase.Candidats candidats(@PathVariable UUID id, @PathVariable String code, Authentication auth) {
        return instances.candidats(appelant(auth), id, code);
    }

    @PostMapping("/{id}/suspension")
    @PreAuthorize(Jetons.ACTIVITE)
    public InstanceVue suspendre(@PathVariable UUID id, @RequestBody(required = false) MotifRequete r, Authentication auth) {
        return InstanceVue.de(instances.suspendre(appelant(auth), id, r == null ? null : r.motif()));
    }

    @PostMapping("/{id}/reprise")
    @PreAuthorize(Jetons.ACTIVITE)
    public InstanceVue reprendre(@PathVariable UUID id, @RequestBody(required = false) MotifRequete r, Authentication auth) {
        return InstanceVue.de(instances.reprendre(appelant(auth), id, r == null ? null : r.motif()));
    }

    @PostMapping("/{id}/annulation")
    @PreAuthorize(Jetons.ACTIVITE)
    public InstanceVue annuler(@PathVariable UUID id, @RequestBody(required = false) MotifRequete r, Authentication auth) {
        return InstanceVue.de(instances.annuler(appelant(auth), id, r == null ? null : r.motif()));
    }
}
