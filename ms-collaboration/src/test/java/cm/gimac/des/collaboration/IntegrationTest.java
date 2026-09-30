package cm.gimac.des.collaboration;

import cm.gimac.des.collaboration.config.AsyncSyncConfiguration;
import cm.gimac.des.collaboration.config.DatabaseTestcontainer;
import cm.gimac.des.collaboration.config.TestSecurityConfiguration;
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
        CollaborationApp.class,
        AsyncSyncConfiguration.class,
        TestSecurityConfiguration.class,
        cm.gimac.des.collaboration.config.JacksonHibernateConfiguration.class,
        DatabaseTestcontainer.class,
    }
)
public @interface IntegrationTest {}
