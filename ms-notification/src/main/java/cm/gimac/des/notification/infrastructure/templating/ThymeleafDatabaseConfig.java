package cm.gimac.des.notification.infrastructure.templating;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.templateresolver.StringTemplateResolver;

@ConditionalOnProperty(name = "gimac.notification.modeles.actif", havingValue = "true")
@Configuration
public class ThymeleafDatabaseConfig {

    @Bean
    public StringTemplateResolver databaseTemplateResolver() {
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setCacheable(true);
        return resolver;
    }
}
