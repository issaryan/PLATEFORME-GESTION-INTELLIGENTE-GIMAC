package cm.gimac.des.participant.config;

import cm.gimac.des.participant.application.AdhesionProperties;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Horloge et paramètres du domaine ; les services de domaine purs n'ont pas d'annotation Spring. */
@Configuration
@EnableConfigurationProperties(AdhesionProperties.class)
public class DomainServicesConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
