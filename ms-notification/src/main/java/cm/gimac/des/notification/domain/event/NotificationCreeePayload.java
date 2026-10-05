package cm.gimac.des.notification.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Donnée de notification.inapp-created.v1 (sujet notification.inapp, clé = notification ; D-80) :
 * le seul destinataire, le type, l'objet désigné et le nombre regroupé, jamais de contenu. Publiée à la
 * création et à chaque regroupement d'une notification ; la passerelle la relaie au seul destinataire.
 */
public record NotificationCreeePayload(
    UUID notificationId,
    String type,
    List<String> destinataires,
    String referenceService,
    String referenceType,
    String referenceId,
    String acteurId,
    int nombre,
    Instant le
) {
    public static final String TYPE = "notification.inapp-created.v1";

    public NotificationCreeePayload {
        destinataires = List.copyOf(destinataires);
    }
}
