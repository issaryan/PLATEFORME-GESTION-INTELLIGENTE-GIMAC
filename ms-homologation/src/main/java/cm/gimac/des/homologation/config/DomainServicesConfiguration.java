package cm.gimac.des.homologation.config;

import cm.gimac.des.homologation.domain.service.ReferentialCompletenessChecker;
import cm.gimac.des.homologation.domain.service.ReferentialGraphValidator;
import cm.gimac.des.homologation.domain.service.TopologicalSorter;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Services de domaine purs déclarés comme beans : ils ne portent aucune annotation Spring (PI HOM-02 A2). */
@Configuration
public class DomainServicesConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public TopologicalSorter topologicalSorter() {
        return new TopologicalSorter();
    }

    @Bean
    public ReferentialGraphValidator referentialGraphValidator(TopologicalSorter sorter) {
        return new ReferentialGraphValidator(sorter);
    }

    @Bean
    public cm.gimac.des.homologation.domain.service.LiaisonOpeningPolicy liaisonOpeningPolicy() {
        return new cm.gimac.des.homologation.domain.service.LiaisonOpeningPolicy();
    }

    @Bean
    public cm.gimac.des.homologation.domain.service.ParcoursProjection parcoursProjection() {
        return new cm.gimac.des.homologation.domain.service.ParcoursProjection();
    }

    @Bean
    public cm.gimac.des.homologation.domain.service.TableauDeBordCalculateur tableauDeBordCalculateur(cm.gimac.des.homologation.domain.service.ParcoursProjection projection) {
        return new cm.gimac.des.homologation.domain.service.TableauDeBordCalculateur(projection);
    }

    @Bean
    public ReferentialCompletenessChecker referentialCompletenessChecker() {
        return new ReferentialCompletenessChecker();
    }
}
