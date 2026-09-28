package cm.gimac.des.incident;

import cm.gimac.des.incident.config.AsyncSyncConfiguration;
import cm.gimac.des.incident.config.EmbeddedKafka;
import cm.gimac.des.incident.config.EmbeddedRedis;
import cm.gimac.des.incident.config.EmbeddedSQL;
import cm.gimac.des.incident.config.JacksonConfiguration;
import cm.gimac.des.incident.config.TestSecurityConfiguration;
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
@SpringBootTest(classes = { IncidentApp.class, JacksonConfiguration.class, AsyncSyncConfiguration.class, TestSecurityConfiguration.class })
@EmbeddedRedis
@EmbeddedSQL
@EmbeddedKafka
public @interface IntegrationTest {
}
