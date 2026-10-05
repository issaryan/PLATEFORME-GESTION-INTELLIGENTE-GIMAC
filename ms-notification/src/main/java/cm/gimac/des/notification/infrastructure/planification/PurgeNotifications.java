package cm.gimac.des.notification.infrastructure.planification;

import cm.gimac.des.notification.application.port.in.RecevoirEvenementsUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Purge planifiée des notifications de plus de 90 jours (docs/lot1/notifications.md §2) : chaque nuit,
 * 3 h 17 UTC par défaut (gimac.notification.purge.cron). Sans effet dans les profils de test, où la
 * planification est désactivée.
 */
@Component
public class PurgeNotifications {

    private final RecevoirEvenementsUseCase notifications;

    public PurgeNotifications(RecevoirEvenementsUseCase notifications) {
        this.notifications = notifications;
    }

    @Scheduled(cron = "${gimac.notification.purge.cron:0 17 3 * * *}", zone = "UTC")
    public void purger() {
        notifications.purger();
    }
}
