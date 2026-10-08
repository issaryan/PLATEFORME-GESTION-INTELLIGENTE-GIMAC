package cm.gimac.des.pilotage.web.rest.activite;

import static cm.gimac.des.pilotage.web.rest.activite.Jetons.appelant;

import cm.gimac.des.pilotage.application.port.in.RapportsUseCase;
import cm.gimac.des.pilotage.application.port.in.RapportsUseCase.DocumentSaisi;
import cm.gimac.des.pilotage.application.port.in.RapportsUseCase.RapportSaisi;
import cm.gimac.des.pilotage.web.rest.activite.Vues.PageVue;
import cm.gimac.des.pilotage.web.rest.activite.Vues.PreremplissageRequete;
import cm.gimac.des.pilotage.web.rest.activite.Vues.PreremplissageVue;
import cm.gimac.des.pilotage.web.rest.activite.Vues.RapportRequete;
import cm.gimac.des.pilotage.web.rest.activite.Vues.RapportVue;
import cm.gimac.des.pilotage.web.rest.activite.Vues.VisaRequete;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.InvalidMediaTypeException;
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
 * Rapports quotidiens et hebdomadaires (docs/lot1/mon-activite.md §2.4), PIL_ACTIVITY_OWN ; vue recus
 * et visa par la chaîne hiérarchique : PIL_TEAM_READ (contrôlé par le cas d'utilisation).
 */
@RestController
@RequestMapping("/api/v1/rapports")
public class RapportResource {

    private final RapportsUseCase rapports;

    public RapportResource(RapportsUseCase rapports) {
        this.rapports = rapports;
    }

    @GetMapping
    @PreAuthorize(Jetons.ACTIVITE)
    public PageVue<RapportVue> lister(
        @RequestParam(name = "vue", required = false) String vue,
        @RequestParam(name = "type", required = false) String type,
        @RequestParam(name = "etat", required = false) String etat,
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "taille", required = false) Integer taille,
        Authentication auth
    ) {
        return PageVue.de(rapports.rapports(appelant(auth), vue, type, etat, page, taille), RapportVue::de);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Jetons.ACTIVITE)
    public RapportVue detail(@PathVariable UUID id, Authentication auth) {
        return RapportVue.de(rapports.rapport(appelant(auth), id));
    }

    /** Contenu proposé pour la période, en markdown ; rien n'est enregistré. */
    @PostMapping("/preremplissage")
    @PreAuthorize(Jetons.ACTIVITE)
    public PreremplissageVue preremplir(@RequestBody(required = false) PreremplissageRequete requete, Authentication auth) {
        return new PreremplissageVue(rapports.preremplir(appelant(auth), requete == null ? null : requete.type(), requete == null ? null : requete.periode()));
    }

    @PostMapping
    @PreAuthorize(Jetons.ACTIVITE)
    public ResponseEntity<RapportVue> creer(@RequestBody(required = false) RapportRequete requete, Authentication auth) {
        RapportVue vue = RapportVue.de(rapports.creer(appelant(auth), saisie(requete)));
        return ResponseEntity.created(URI.create("/api/v1/rapports/" + vue.id())).body(vue);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Jetons.ACTIVITE)
    public RapportVue modifier(@PathVariable UUID id, @RequestBody(required = false) RapportRequete requete, Authentication auth) {
        return RapportVue.de(rapports.modifier(appelant(auth), id, saisie(requete)));
    }

    @PostMapping("/{id}/soumission")
    @PreAuthorize(Jetons.ACTIVITE)
    public RapportVue soumettre(@PathVariable UUID id, @RequestBody(required = false) Vues.SoumissionRequete requete, Authentication auth) {
        return RapportVue.de(rapports.soumettre(appelant(auth), id, requete == null ? null : requete.destinataireId()));
    }

    /** Destinataire qui recevra le rapport s'il est soumis maintenant, et choix possibles dans la chaîne hiérarchique (auteur). */
    @GetMapping("/{id}/destinataires")
    @PreAuthorize(Jetons.ACTIVITE)
    public Vues.DestinatairesVue destinataires(@PathVariable UUID id, Authentication auth) {
        return Vues.DestinatairesVue.de(rapports.destinataires(appelant(auth), id));
    }

    @PostMapping("/{id}/visa")
    @PreAuthorize(Jetons.ACTIVITE)
    public RapportVue viser(@PathVariable UUID id, @RequestBody(required = false) VisaRequete requete, Authentication auth) {
        return RapportVue.de(rapports.viser(appelant(auth), id, requete == null ? null : requete.commentaire()));
    }

    /**
     * Document joint, relayé en flux depuis MS-DOCUMENT (jamais chargé en entier) : auteur,
     * destinataire et chaîne hiérarchique.
     */
    @GetMapping("/{id}/document")
    @PreAuthorize(Jetons.ACTIVITE)
    public ResponseEntity<Resource> document(@PathVariable UUID id, Authentication auth) {
        RapportsUseCase.DocumentLu d = rapports.document(appelant(auth), id);
        MediaType type;
        try {
            type = MediaType.parseMediaType(d.typeMime());
        } catch (InvalidMediaTypeException e) {
            type = MediaType.APPLICATION_OCTET_STREAM;
        }
        String nom = d.nom() == null || d.nom().isBlank() ? "rapport" : d.nom();
        ResponseEntity.BodyBuilder r = ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nom, StandardCharsets.UTF_8).build().toString())
            .header("X-Content-Type-Options", "nosniff")
            .contentType(type);
        if (d.taille() >= 0) {
            r = r.contentLength(d.taille());
        }
        return r.body(new InputStreamResource(d.flux()));
    }

    private static RapportSaisi saisie(RapportRequete r) {
        return r == null
            ? null
            : new RapportSaisi(r.type(), r.periode(), r.mode(), r.contenu(), r.document() == null ? null : new DocumentSaisi(r.document().ticket()));
    }
}
