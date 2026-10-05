package cm.gimac.des.notification.web.rest.boite;

import static cm.gimac.des.notification.web.rest.boite.Jetons.compte;

import cm.gimac.des.notification.application.port.in.BoiteNotificationsUseCase;
import cm.gimac.des.notification.application.port.in.BoiteNotificationsUseCase.ReferenceSaisie;
import cm.gimac.des.notification.web.rest.boite.Vues.CompteurVue;
import cm.gimac.des.notification.web.rest.boite.Vues.LectureRequete;
import cm.gimac.des.notification.web.rest.boite.Vues.NotificationVue;
import cm.gimac.des.notification.web.rest.boite.Vues.PageVue;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Notifications du compte appelant (docs/lot1/notifications.md §3), exposées par la passerelle sous
 * /bff/{des,console}/notification/notifications : liste, compteur, lecture d'une notification ou de
 * plusieurs. Jeton à deux facteurs (SecurityConfiguration) puis NOT_INBOX_READ ; la notification d'un
 * autre compte répond 404.
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class BoiteNotificationsResource {

    private final BoiteNotificationsUseCase boite;

    public BoiteNotificationsResource(BoiteNotificationsUseCase boite) {
        this.boite = boite;
    }

    @GetMapping
    @PreAuthorize(Jetons.BOITE)
    public PageVue<NotificationVue> lister(
        @RequestParam(name = "etat", required = false) String etat,
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "taille", required = false) Integer taille,
        Authentication auth
    ) {
        return PageVue.de(boite.lister(compte(auth), etat, page, taille), NotificationVue::de);
    }

    @GetMapping("/compteur")
    @PreAuthorize(Jetons.BOITE)
    public CompteurVue compteur(Authentication auth) {
        return new CompteurVue(boite.nonLues(compte(auth)));
    }

    @PostMapping("/{id}/lecture")
    @PreAuthorize(Jetons.BOITE)
    public ResponseEntity<Void> marquerLue(@PathVariable UUID id, Authentication auth) {
        boite.marquerLue(compte(auth), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/lecture")
    @PreAuthorize(Jetons.BOITE)
    public ResponseEntity<Void> marquerLues(@RequestBody(required = false) LectureRequete requete, Authentication auth) {
        ReferenceSaisie reference = requete == null || requete.reference() == null
            ? null
            : new ReferenceSaisie(requete.reference().service(), requete.reference().type(), requete.reference().id());
        boite.marquerLues(compte(auth), requete == null ? null : requete.type(), reference);
        return ResponseEntity.noContent().build();
    }
}
