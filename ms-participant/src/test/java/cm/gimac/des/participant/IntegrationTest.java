package cm.gimac.des.participant;

import cm.gimac.des.participant.config.AsyncSyncConfiguration;
import cm.gimac.des.participant.config.DatabaseTestcontainer;
import cm.gimac.des.participant.config.RedisTestContainer;
import cm.gimac.des.participant.config.TestSecurityConfiguration;
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
        ParticipantApp.class,
        AsyncSyncConfiguration.class,
        TestSecurityConfiguration.class,
        cm.gimac.des.participant.config.JacksonHibernateConfiguration.class,
        DatabaseTestcontainer.class,
        RedisTestContainer.class,
    }
)
public @interface IntegrationTest {}
