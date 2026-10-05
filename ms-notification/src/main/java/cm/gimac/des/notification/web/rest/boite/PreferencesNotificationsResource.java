package cm.gimac.des.notification.web.rest.boite;

import static cm.gimac.des.notification.web.rest.boite.Jetons.compte;

import cm.gimac.des.notification.application.port.in.BoiteNotificationsUseCase;
import cm.gimac.des.notification.application.port.in.BoiteNotificationsUseCase.PreferencesSaisie;
import cm.gimac.des.notification.web.rest.boite.Vues.PreferencesRequete;
import cm.gimac.des.notification.web.rest.boite.Vues.PreferencesVue;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Préférences de notification du compte appelant (docs/lot1/notifications.md §3) :
 * { systeme, son, types: { MESSAGE, TACHE, RAPPORT } } ; défauts systeme=true, son=false, tous les types
 * actifs. NOT_INBOX_READ.
 */
@RestController
@RequestMapping("/api/v1/preferences")
public class PreferencesNotificationsResource {

    private final BoiteNotificationsUseCase boite;

    public PreferencesNotificationsResource(BoiteNotificationsUseCase boite) {
        this.boite = boite;
    }

    @GetMapping
    @PreAuthorize(Jetons.BOITE)
    public PreferencesVue lire(Authentication auth) {
        return PreferencesVue.de(boite.preferences(compte(auth)));
    }

    @PutMapping
    @PreAuthorize(Jetons.BOITE)
    public PreferencesVue modifier(@RequestBody(required = false) PreferencesRequete requete, Authentication auth) {
        PreferencesSaisie saisie = requete == null ? null : new PreferencesSaisie(requete.systeme(), requete.son(), requete.types());
        return PreferencesVue.de(boite.modifierPreferences(compte(auth), saisie));
    }
}
