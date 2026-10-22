package cm.gimac.des.iam.infrastructure.scheduling;

import cm.gimac.des.iam.application.usecase.sandbox.DesactivationInactifsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Passage quotidien de la désactivation des comptes inactifs de la sandbox (D-119, US-EXI-23), à 2 h 30 par défaut
 * (gimac.sandbox.inactivite-planification) ; désactivable (gimac.sandbox.inactivite-active=false).
 */
@Component
@ConditionalOnProperty(name = "gimac.sandbox.inactivite-active", havingValue = "true", matchIfMissing = true)
public class DesactivationInactifsPlanifiee {

    private static final Logger LOG = LoggerFactory.getLogger(DesactivationInactifsPlanifiee.class);

    private final DesactivationInactifsService service;

    public DesactivationInactifsPlanifiee(DesactivationInactifsService service) {
        this.service = service;
    }

    @Scheduled(cron = "${gimac.sandbox.inactivite-planification:0 30 2 * * *}")
    public void executer() {
        DesactivationInactifsService.Bilan b = service.executer();
        LOG.info("Comptes inactifs de la sandbox : {} désactivés, {} écartés (protégés), passage exécuté : {}", b.desactives(), b.ecartes(), b.execute());
    }
}
