package cm.gimac.des.notification.config;

import cm.gimac.des.notification.domain.service.ReglesNotifications;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Services de domaine purs et horloge des cas d'utilisation (remplaçable en test). */
@Configuration
public class DomainServicesConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public ReglesNotifications reglesNotifications() {
        return new ReglesNotifications();
    }
}
