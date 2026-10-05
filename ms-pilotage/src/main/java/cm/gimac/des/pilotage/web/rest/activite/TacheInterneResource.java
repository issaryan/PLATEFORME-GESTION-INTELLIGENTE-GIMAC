package cm.gimac.des.pilotage.web.rest.activite;

import cm.gimac.des.pilotage.application.port.in.TachesUseCase;
import cm.gimac.des.pilotage.application.port.in.TachesUseCase.Origine;
import cm.gimac.des.pilotage.application.port.in.TachesUseCase.TacheInterne;
import cm.gimac.des.pilotage.web.rest.activite.Vues.TacheInterneRequete;
import cm.gimac.des.pilotage.web.rest.activite.Vues.TacheVue;
import cm.gimac.des.pilotage.application.port.in.TachesUseCase.CibleInterne;
import cm.gimac.des.pilotage.web.rest.activite.Vues.CibleInterneRequete;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tâche créée par un autre service (§2.3 ; D-77 : homologation-service), hors passerelle : jeton de
 * service portant INTERNAL_PILOTAGE_TASK_CREATE. Idempotente par origine.reference + responsableId :
 * 201 à la création, 200 avec la tâche existante ensuite. Le même service termine ou rouvre les tâches qu'il
 * a créées ({@code /cloture}, {@code /reouverture}, D-77) : 200 avec les tâches visées, éventuellement aucune.
 */
@RestController
@RequestMapping("/api/v1/interne/taches")
public class TacheInterneResource {

    private final TachesUseCase taches;

    public TacheInterneResource(TachesUseCase taches) {
        this.taches = taches;
    }

    @PostMapping
    @PreAuthorize(Jetons.CREATION_INTERNE)
    public ResponseEntity<TacheVue> creer(@RequestBody(required = false) TacheInterneRequete r, Authentication auth) {
        String client = Jetons.client(auth);
        TacheInterne demande = r == null
            ? null
            : new TacheInterne(
                r.titre(),
                r.description(),
                r.echeance(),
                r.priorite(),
                r.responsableId(),
                r.createurId(),
                r.origine() == null ? null : new Origine(r.origine().type(), r.origine().reference(), r.origine().service(), r.origine().lien())
            );
        TachesUseCase.Creation c = taches.creerPourService(client, demande);
        TacheVue vue = TacheVue.de(c.tache());
        return ResponseEntity.status(c.creee() ? HttpStatus.CREATED : HttpStatus.OK).location(URI.create("/api/v1/taches/" + vue.id())).body(vue);
    }

    /** Tâches de l'origine (et du responsable, s'il est donné) terminées : opération faite, sans objet, référentiel publié. */
    @PostMapping("/cloture")
    @PreAuthorize(Jetons.CREATION_INTERNE)
    public List<TacheVue> cloturer(@RequestBody(required = false) CibleInterneRequete r, Authentication auth) {
        return taches.terminerPourService(Jetons.client(auth), cible(r)).stream().map(TacheVue::de).toList();
    }

    /** Tâches de l'origine (et du responsable, s'il est donné) rouvertes : opération reprise. */
    @PostMapping("/reouverture")
    @PreAuthorize(Jetons.CREATION_INTERNE)
    public List<TacheVue> rouvrir(@RequestBody(required = false) CibleInterneRequete r, Authentication auth) {
        return taches.rouvrirPourService(Jetons.client(auth), cible(r)).stream().map(TacheVue::de).toList();
    }

    private static CibleInterne cible(CibleInterneRequete r) {
        return r == null
            ? null
            : new CibleInterne(r.origine() == null ? null : new Origine(r.origine().type(), r.origine().reference(), r.origine().service(), r.origine().lien()), r.responsableId());
    }
}
