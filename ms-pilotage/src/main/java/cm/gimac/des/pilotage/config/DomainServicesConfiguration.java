package cm.gimac.des.pilotage.config;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Horloge des cas d'utilisation (remplaçable en test) ; les jours sont lus dans le fuseau Africa/Douala. */
@Configuration
public class DomainServicesConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
