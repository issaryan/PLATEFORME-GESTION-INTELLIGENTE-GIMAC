package cm.gimac.des.monitoring.infrastructure.ordonnanceur;

import cm.gimac.des.monitoring.application.port.in.ReceptionAlertesUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Adaptateur pilote de l'ordonnanceur (ShiftSchedulerAdapter du CC) : échéances d'acquittement et escalade des alertes
 * non acquittées (MON-05), toutes les 30 s par défaut (gimac.monitoring.escalade.intervalle, en millisecondes).
 */
@Component
@ConditionalOnProperty(name = "gimac.monitoring.escalade.actif", havingValue = "true", matchIfMissing = true)
public class OrdonnanceurEscalade {

    private static final Logger LOG = LoggerFactory.getLogger(OrdonnanceurEscalade.class);

    private final ReceptionAlertesUseCase alertes;

    public OrdonnanceurEscalade(ReceptionAlertesUseCase alertes) {
        this.alertes = alertes;
    }

    @Scheduled(fixedDelayString = "${gimac.monitoring.escalade.intervalle:30000}", initialDelayString = "${gimac.monitoring.escalade.intervalle:30000}")
    public void escalader() {
        try {
            int n = alertes.escalader();
            if (n > 0) {
                LOG.info("Escalade : {} alerte(s) montée(s) d'un palier", n);
            }
        } catch (RuntimeException e) {
            LOG.warn("Escalade des alertes en échec : {}", e.toString());
        }
    }
}
