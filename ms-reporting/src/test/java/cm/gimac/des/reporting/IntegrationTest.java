package cm.gimac.des.reporting;

import cm.gimac.des.reporting.config.AsyncSyncConfiguration;
import cm.gimac.des.reporting.config.EmbeddedKafka;
import cm.gimac.des.reporting.config.EmbeddedRedis;
import cm.gimac.des.reporting.config.EmbeddedSQL;
import cm.gimac.des.reporting.config.JacksonConfiguration;
import cm.gimac.des.reporting.config.TestSecurityConfiguration;
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
@SpringBootTest(classes = { ReportingApp.class, JacksonConfiguration.class, AsyncSyncConfiguration.class, TestSecurityConfiguration.class })
@EmbeddedRedis
@EmbeddedSQL
@EmbeddedKafka
public @interface IntegrationTest {
}
