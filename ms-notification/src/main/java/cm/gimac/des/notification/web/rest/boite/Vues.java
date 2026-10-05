package cm.gimac.des.notification.web.rest.boite;

import cm.gimac.des.notification.application.port.in.BoiteNotificationsUseCase.Page;
import cm.gimac.des.notification.domain.boite.NotificationUtilisateur;
import cm.gimac.des.notification.domain.boite.PreferencesNotification;
import cm.gimac.des.notification.domain.boite.TypeNotification;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/** Représentations JSON du contrat docs/lot1/notifications.md §2 et §3. */
final class Vues {

    private Vues() {}

    record PageVue<T>(List<T> contenu, long total, int page, int taille) {
        static <S, T> PageVue<T> de(Page<S> p, Function<S, T> vue) {
            return new PageVue<>(p.contenu().stream().map(vue).toList(), p.total(), p.page(), p.taille());
        }
    }

    record ReferenceVue(String service, String type, String id) {}

    record ActeurVue(String id, String nomAffiche) {}

    record NotificationVue(
        UUID id,
        String type,
        String titre,
        String sousTitre,
        String lien,
        ReferenceVue reference,
        ActeurVue acteur,
        int nombre,
        Instant creeLe,
        Instant misAJourLe,
        Instant lueLe
    ) {
        static NotificationVue de(NotificationUtilisateur n) {
            return new NotificationVue(
                n.getId(),
                n.getType().name(),
                n.getTitre(),
                n.getSousTitre(),
                n.getLien(),
                new ReferenceVue(n.getReference().getService(), n.getReference().getType(), n.getReference().getId()),
                n.getActeurId() == null ? null : new ActeurVue(n.getActeurId(), n.getActeurNom()),
                n.getNombre(),
                n.getCreeLe(),
                n.getMisAJourLe(),
                n.getLueLe()
            );
        }
    }

    record CompteurVue(long nonLues) {}

    record LectureRequete(String type, ReferenceVue reference) {}

    record PreferencesVue(boolean systeme, boolean son, Map<String, Boolean> types) {
        static PreferencesVue de(PreferencesNotification p) {
            Map<String, Boolean> types = new LinkedHashMap<>();
            for (TypeNotification t : TypeNotification.values()) {
                types.put(t.name(), p.accepte(t));
            }
            return new PreferencesVue(p.isSysteme(), p.isSon(), types);
        }
    }

    record PreferencesRequete(Boolean systeme, Boolean son, Map<String, Boolean> types) {}
}
