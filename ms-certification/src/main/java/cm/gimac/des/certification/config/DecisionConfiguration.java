package cm.gimac.des.certification.config;

import java.time.Clock;
import cm.gimac.des.certification.application.usecase.ParametresCircuits;
import cm.gimac.des.certification.application.usecase.ParametresOperations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Horloge des verrous de la décision (instants d'établissement, de signature et d'horodatage), UTC ; paramètres des
 * circuits de signature (D-94) et du contrôle des gestes par l'opération du parcours (D-93 §4).
 */
@Configuration
@EnableConfigurationProperties({ ParametresCircuits.class, ParametresOperations.class })
public class DecisionConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Clock horloge() {
        return Clock.systemUTC();
    }
}
