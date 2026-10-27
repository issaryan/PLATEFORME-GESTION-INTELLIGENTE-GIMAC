package cm.gimac.des.iam.infrastructure.scheduling;

import cm.gimac.des.iam.application.usecase.sandbox.ReexamensAccesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Ouverture automatique au premier jour de chaque trimestre, à 3 h (heure du serveur). */
@Component
@ConditionalOnProperty(name = "gimac.iam.reexamen-actif", havingValue = "true", matchIfMissing = true)
public class ReexamenAccesPlanifie {

    private static final Logger LOG = LoggerFactory.getLogger(ReexamenAccesPlanifie.class);

    private final ReexamensAccesService service;

    public ReexamenAccesPlanifie(ReexamensAccesService service) {
        this.service = service;
    }

    @Scheduled(cron = "${gimac.iam.reexamen-planification:0 0 3 1 1,4,7,10 *}")
    public void executer() {
        LOG.info("Campagne trimestrielle de réexamen ouverte : {}", service.ouvrirTrimestre());
    }
}
