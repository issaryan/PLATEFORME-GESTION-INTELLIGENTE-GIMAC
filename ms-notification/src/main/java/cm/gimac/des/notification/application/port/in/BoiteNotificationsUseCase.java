package cm.gimac.des.notification.application.port.in;

import cm.gimac.des.notification.domain.boite.NotificationUtilisateur;
import cm.gimac.des.notification.domain.boite.PreferencesNotification;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Boîte de notifications du compte appelant (docs/lot1/notifications.md §3) : liste, compteur, lecture et
 * préférences. La notification d'un autre compte répond 404 NOT_NOTIFICATION_INTROUVABLE.
 */
public interface BoiteNotificationsUseCase {
    record Page<T>(List<T> contenu, long total, int page, int taille) {
        public Page {
            contenu = List.copyOf(contenu);
        }
    }

    /** Objet désigné par un marquage en lecture (tous les champs sont exigés s'il est fourni). */
    record ReferenceSaisie(String service, String type, String id) {}

    /** Préférences saisies : un champ absent garde sa valeur. */
    record PreferencesSaisie(Boolean systeme, Boolean son, Map<String, Boolean> types) {}

    /** Notifications récentes d'abord ; etat : non-lues | toutes (défaut). */
    Page<NotificationUtilisateur> lister(String compte, String etat, Integer page, Integer taille);

    long nonLues(String compte);

    void marquerLue(String compte, UUID id);

    /** Toutes les non-lues, ou celles d'un type et d'un objet ; rend le nombre marqué. */
    int marquerLues(String compte, String type, ReferenceSaisie reference);

    PreferencesNotification preferences(String compte);

    PreferencesNotification modifierPreferences(String compte, PreferencesSaisie saisie);
}
