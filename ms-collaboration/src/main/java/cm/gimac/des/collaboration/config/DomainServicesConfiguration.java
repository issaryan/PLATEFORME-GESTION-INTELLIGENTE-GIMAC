package cm.gimac.des.collaboration.config;

import cm.gimac.des.collaboration.domain.service.MasquagePan;
import cm.gimac.des.collaboration.domain.service.ReglesMessagerie;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Services de domaine purs déclarés comme beans : ils ne portent aucune annotation Spring. */
@Configuration
public class DomainServicesConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public MasquagePan masquagePan() {
        return new MasquagePan();
    }

    @Bean
    public ReglesMessagerie reglesMessagerie() {
        return new ReglesMessagerie();
    }
}
