package cm.gimac.des.certification.config;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Horloge des verrous de la décision (instants d'établissement, de signature et d'horodatage), UTC. */
@Configuration
public class DecisionConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Clock horloge() {
        return Clock.systemUTC();
    }
}
