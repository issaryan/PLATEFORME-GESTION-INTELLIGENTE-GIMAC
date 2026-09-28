package cm.gimac.des.audit;

import cm.gimac.des.audit.config.AsyncSyncConfiguration;
import cm.gimac.des.audit.config.DatabaseTestcontainer;
import cm.gimac.des.audit.config.TestSecurityConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
    classes = {
        AuditApp.class,
        AsyncSyncConfiguration.class,
        TestSecurityConfiguration.class,
        cm.gimac.des.audit.config.JacksonHibernateConfiguration.class,
        DatabaseTestcontainer.class,
    }
)
public @interface IntegrationTest {}
