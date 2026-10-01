package cm.gimac.des.gateway.config;

import reactor.blockhound.BlockHound;
import reactor.blockhound.integration.BlockHoundIntegration;

public class JHipsterBlockHoundIntegration implements BlockHoundIntegration {

    @Override
    public void applyTo(BlockHound.Builder builder) {
        builder.allowBlockingCallsInside("java.util.UUID", "randomUUID");
        builder.allowBlockingCallsInside("org.springframework.validation.beanvalidation.SpringValidatorAdapter", "validate");
        builder.allowBlockingCallsInside("cm.gimac.des.gateway.service.MailService", "sendEmailFromTemplate");
        builder.allowBlockingCallsInside("cm.gimac.des.gateway.security.DomainUserDetailsService", "createSpringSecurityUser");
        builder.allowBlockingCallsInside("org.springframework.web.reactive.result.method.InvocableHandlerMethod", "invoke");
        builder.allowBlockingCallsInside("org.springdoc.core.service.OpenAPIService", "build");
        builder.allowBlockingCallsInside("org.springdoc.core.service.OpenAPIService", "getWebhooksClasses");
        builder.allowBlockingCallsInside("org.springdoc.core.service.AbstractRequestService", "build");
        // Jackson 3 : la première création d'un désérialiseur prend un verrou court (ReentrantLock) du cache ;
        // deux décodages concurrents sur des fils Netty (sondes de la supervision) faisaient signaler une
        // attente de verrou, sans entrée-sortie, comme appel bloquant (échec intermittent sous charge).
        builder.allowBlockingCallsInside("tools.jackson.databind.deser.DeserializerCache", "_createAndCacheValueDeserializer");
        // jhipster-needle-blockhound-integration - JHipster will add additional gradle plugins here
    }
}
