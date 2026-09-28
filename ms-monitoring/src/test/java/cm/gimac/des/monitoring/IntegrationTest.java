package cm.gimac.des.monitoring;

import cm.gimac.des.monitoring.config.AsyncSyncConfiguration;
import cm.gimac.des.monitoring.config.EmbeddedKafka;
import cm.gimac.des.monitoring.config.EmbeddedRedis;
import cm.gimac.des.monitoring.config.EmbeddedSQL;
import cm.gimac.des.monitoring.config.JacksonConfiguration;
import cm.gimac.des.monitoring.config.TestSecurityConfiguration;
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
    classes = { MonitoringApp.class, JacksonConfiguration.class, AsyncSyncConfiguration.class, TestSecurityConfiguration.class }
)
@EmbeddedRedis
@EmbeddedSQL
@EmbeddedKafka
public @interface IntegrationTest {
}
