package cm.gimac.des.notification;

import cm.gimac.des.notification.config.AsyncSyncConfiguration;
import cm.gimac.des.notification.config.EmbeddedKafka;
import cm.gimac.des.notification.config.EmbeddedRedis;
import cm.gimac.des.notification.config.EmbeddedSQL;
import cm.gimac.des.notification.config.JacksonConfiguration;
import cm.gimac.des.notification.config.TestSecurityConfiguration;
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
    classes = { NotificationApp.class, JacksonConfiguration.class, AsyncSyncConfiguration.class, TestSecurityConfiguration.class }
)
@EmbeddedRedis
@EmbeddedSQL
@EmbeddedKafka
public @interface IntegrationTest {
}
