package cm.gimac.des.notification.config;

import cm.gimac.des.notification.domain.service.ReglesHomologation;
import cm.gimac.des.notification.domain.service.ReglesNotifications;
import cm.gimac.des.notification.domain.service.ReglesSandbox;
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

    /** Notifications du parcours d'homologation (D-93). */
    @Bean
    public ReglesHomologation reglesHomologation() {
        return new ReglesHomologation();
    }

    /** Règles et gabarits des notifications de la Sandbox GIMAC (US-EXI-09). */
    @Bean
    public ReglesSandbox reglesSandbox() {
        return cm.gimac.des.notification.infrastructure.catalogue.CatalogueNotificationsSandbox.regles();
    }
}
