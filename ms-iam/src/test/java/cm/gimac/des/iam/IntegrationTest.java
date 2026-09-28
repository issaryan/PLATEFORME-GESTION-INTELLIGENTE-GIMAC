package cm.gimac.des.iam;

import cm.gimac.des.iam.config.AsyncSyncConfiguration;
import cm.gimac.des.iam.config.DatabaseTestcontainer;
import cm.gimac.des.iam.config.RedisTestContainer;
import cm.gimac.des.iam.config.TestSecurityConfiguration;
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
        IamApp.class,
        AsyncSyncConfiguration.class,
        TestSecurityConfiguration.class,
        cm.gimac.des.iam.config.JacksonHibernateConfiguration.class,
        DatabaseTestcontainer.class,
        RedisTestContainer.class,
    }
)
public @interface IntegrationTest {}
