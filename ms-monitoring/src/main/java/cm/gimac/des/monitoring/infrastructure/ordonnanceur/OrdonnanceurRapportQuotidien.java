package cm.gimac.des.monitoring.infrastructure.ordonnanceur;

import cm.gimac.des.monitoring.application.usecase.AvisRapportQuotidien;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Ordonnanceur du rapport quotidien de 8 h (D-116 §10.2) : rappel aux membres de la vacation de nuit, puis
 * signalement du retard aux chefs ; toutes les 5 min par défaut (gimac.monitoring.rapport-quotidien.intervalle).
 */
@Component
@ConditionalOnProperty(name = "gimac.monitoring.rapport-quotidien.actif", havingValue = "true", matchIfMissing = true)
public class OrdonnanceurRapportQuotidien {

    private static final Logger LOG = LoggerFactory.getLogger(OrdonnanceurRapportQuotidien.class);

    private final AvisRapportQuotidien avis;

    public OrdonnanceurRapportQuotidien(AvisRapportQuotidien avis) {
        this.avis = avis;
    }

    @Scheduled(fixedDelayString = "${gimac.monitoring.rapport-quotidien.intervalle:300000}", initialDelayString = "${gimac.monitoring.rapport-quotidien.intervalle:300000}")
    public void verifier() {
        try {
            List<String> emis = avis.verifier();
            if (!emis.isEmpty()) {
                LOG.info("Rapport quotidien : avis émis {}", emis);
            }
        } catch (RuntimeException e) {
            LOG.warn("Suivi du rapport quotidien en échec : {}", e.toString());
        }
    }
}
