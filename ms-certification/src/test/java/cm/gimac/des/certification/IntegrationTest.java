package cm.gimac.des.certification;

import cm.gimac.des.certification.config.AsyncSyncConfiguration;
import cm.gimac.des.certification.config.DatabaseTestcontainer;
import cm.gimac.des.certification.config.TestSecurityConfiguration;
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
        CertificationApp.class,
        AsyncSyncConfiguration.class,
        TestSecurityConfiguration.class,
        cm.gimac.des.certification.config.JacksonHibernateConfiguration.class,
        DatabaseTestcontainer.class,
    }
)
public @interface IntegrationTest {}
