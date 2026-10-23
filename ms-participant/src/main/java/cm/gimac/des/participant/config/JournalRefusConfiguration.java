package cm.gimac.des.participant.config;

import cm.gimac.des.participant.infrastructure.security.JournalRefusSandbox;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationEventPublisher;
import org.springframework.security.authorization.SpringAuthorizationEventPublisher;

/**
 * Publication des refus d'autorisation (routes et @PreAuthorize) en événements Spring, écoutés par le journal des
 * tentatives des comptes de service de la sandbox (D-120, US-EXI-21). Les autorisations accordées ne sont pas publiées.
 */
@Configuration
public class JournalRefusConfiguration {

    @Bean
    public AuthorizationEventPublisher authorizationEventPublisher(ApplicationEventPublisher publication) {
        return new SpringAuthorizationEventPublisher(publication);
    }

    @Bean
    public JournalRefusSandbox journalRefusSandbox() {
        return new JournalRefusSandbox();
    }
}
