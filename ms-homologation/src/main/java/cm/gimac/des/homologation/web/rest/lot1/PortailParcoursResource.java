package cm.gimac.des.homologation.web.rest.lot1;

import cm.gimac.des.homologation.application.usecase.LiaisonQueries;
import cm.gimac.des.homologation.application.usecase.ParcoursCommandes;
import cm.gimac.des.homologation.application.usecase.ParcoursLectures;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.parcours.Acteur;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.Vues;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Portail du participant (qualité Externe, CR §3.4), exposé sous /bff/portail/homologation : ses
 * liaisons, le dossier projeté sans nom interne, ses tâches, les coordonnées techniques d'essai
 * (rang 12), ses livrables et la fiche de suivi (rang 32). Cloisonné à l'institution du jeton : une
 * liaison d'une autre institution est introuvable (404).
 */
@RestController
@RequestMapping("/api/v1/portail")
public class PortailParcoursResource {

    public record CoordonneesRequest(@Size(max = 253) String adresse, @Size(max = 5) String port, @Size(max = 500) String pointsTerminaison) {}

    public record LigneRequest(String serviceCode, Boolean ouvert, @Size(max = 500) String commentaire) {}

    public record FicheRequest(@NotNull List<@Valid LigneRequest> lignes) {}

    private final ParcoursLectures lectures;
    private final ParcoursCommandes commandes;
    private final LiaisonQueries liaisons;

    public PortailParcoursResource(ParcoursLectures lectures, ParcoursCommandes commandes, LiaisonQueries liaisons) {
        this.lectures = lectures;
        this.commandes = commandes;
        this.liaisons = liaisons;
    }

    @GetMapping("/liaisons")
    @PreAuthorize("hasAuthority('HOM_PORTAL_READ')")
    public List<LiaisonsCadrageResource.LiaisonVue> liaisons(Authentication auth) {
        Acteur a = ActeurCourant.de(auth);
        return lectures.mesParcours(a).stream().map(p -> LiaisonsCadrageResource.LiaisonVue.dePortail(liaisons.fichePortail(p.liaison(), resume(p)))).toList();
    }

    private Vues.LiaisonResume resume(Parcours p) {
        return lectures.resumePortail(p);
    }

    @GetMapping("/liaisons/{id}/dossier")
    @PreAuthorize("hasAuthority('HOM_PORTAL_READ')")
    public Vues.Dossier dossier(@PathVariable UUID id, Authentication auth) {
        return lectures.dossierPortail(id, ActeurCourant.de(auth));
    }

    @GetMapping("/taches")
    @PreAuthorize("hasAuthority('HOM_PORTAL_READ')")
    public Vues.TableauActeur taches(Authentication auth) {
        return lectures.tachesPortail(ActeurCourant.de(auth));
    }

    @PutMapping("/liaisons/{id}/coordonnees-techniques")
    @PreAuthorize("hasAuthority('HOM_PORTAL_SUBMIT')")
    public ResponseEntity<Void> coordonnees(@PathVariable UUID id, @Valid @RequestBody CoordonneesRequest r, Authentication auth) {
        commandes.communiquerCoordonnees(id, r.adresse(), r.port(), r.pointsTerminaison(), ActeurCourant.de(auth));
        return ResponseEntity.noContent().build();
    }

    @PostMapping(path = "/operations/{id}/livrable", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('HOM_PORTAL_SUBMIT')")
    public ResponseEntity<Void> livrable(
        @PathVariable UUID id,
        @RequestPart(name = "fichier", required = false) MultipartFile fichier,
        @RequestParam(name = "commentaire", required = false) String commentaire,
        @RequestParam(name = "livrable", required = false) String livrable,
        Authentication auth
    ) {
        byte[] contenu;
        try {
            contenu = fichier == null ? null : fichier.getBytes();
        } catch (IOException e) {
            throw HomologationException.unprocessable("HOM_DOCUMENT_INVALID", "Le livrable est illisible.");
        }
        commandes.deposerLivrablePortail(
            id,
            livrable,
            contenu,
            fichier == null ? null : fichier.getOriginalFilename(),
            fichier == null ? null : fichier.getContentType(),
            commentaire,
            ActeurCourant.de(auth)
        );
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/liaisons/{id}/fiche-suivi")
    @PreAuthorize("hasAuthority('HOM_PORTAL_READ')")
    public Vues.FicheSuivi fiche(@PathVariable UUID id, Authentication auth) {
        return lectures.fichePortail(id, ActeurCourant.de(auth));
    }

    @PutMapping("/liaisons/{id}/fiche-suivi")
    @PreAuthorize("hasAuthority('HOM_PORTAL_SUBMIT')")
    public ResponseEntity<Void> renseignerFiche(@PathVariable UUID id, @Valid @RequestBody FicheRequest r, Authentication auth) {
        commandes.renseignerFiche(id, r.lignes().stream().map(l -> new Parcours.LigneFiche(l.serviceCode(), l.ouvert(), l.commentaire())).toList(), ActeurCourant.de(auth));
        return ResponseEntity.noContent().build();
    }
}
