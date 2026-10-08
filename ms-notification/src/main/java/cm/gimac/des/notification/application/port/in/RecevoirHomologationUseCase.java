package cm.gimac.des.notification.application.port.in;

import cm.gimac.des.notification.domain.service.ReglesHomologation.FaitsJalon;
import cm.gimac.des.notification.domain.service.ReglesHomologation.FaitsOperation;
import java.time.Instant;

/**
 * Événements du parcours d'homologation transformés en notifications HOMOLOGATION (D-93,
 * docs/lot1/effet-reel-referentiels.md §8.2 et §11) : une notification par compte visé, selon son rôle RACI et ses
 * préférences, jamais regroupée ; chaque notification publie notification.inapp-created.v1 par l'outbox.
 */
public interface RecevoirHomologationUseCase {
    /**
     * homologation.operation-activated.v1 ou homologation.operation-validation-requested.v1 (sujet
     * homologation.operation). Rend le nombre de notifications créées.
     */
    int operationHomologation(FaitsOperation faits, Instant le);

    /** homologation.liaison-milestone-reached.v1 (sujet homologation.liaison). Rend le nombre de notifications créées. */
    int jalonHomologation(FaitsJalon faits, Instant le);
}
