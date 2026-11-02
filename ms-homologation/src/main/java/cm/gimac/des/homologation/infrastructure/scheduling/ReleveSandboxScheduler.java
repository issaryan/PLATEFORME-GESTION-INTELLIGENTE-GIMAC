package cm.gimac.des.homologation.infrastructure.scheduling;

import cm.gimac.des.homologation.application.usecase.sandbox.ReleveSandbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Relève périodique des résultats de la sandbox (US-EXI-14) : source de vérité, indépendante de la notification de
 * fin de campagne (US-EXI-15). Période proposée par le contrat L04 : cinq minutes (point P11), configurable.
 */
@Component
public class ReleveSandboxScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(ReleveSandboxScheduler.class);

    private final ReleveSandbox releve;

    public ReleveSandboxScheduler(ReleveSandbox releve) {
        this.releve = releve;
    }

    @Scheduled(initialDelayString = "${gimac.sandbox.releve.delai-initial-ms:60000}", fixedDelayString = "${gimac.sandbox.releve.periode-ms:300000}")
    public void relever() {
        try {
            int n = releve.releverTout();
            LOG.info("Relève périodique de la sandbox terminée avec succès ({} liaison(s) synchronisée(s))", n);
        } catch (Throwable t) {
            LOG.error("Erreur inattendue lors de la relève périodique de la sandbox : {}", t.getMessage(), t);
        }
    }
}
