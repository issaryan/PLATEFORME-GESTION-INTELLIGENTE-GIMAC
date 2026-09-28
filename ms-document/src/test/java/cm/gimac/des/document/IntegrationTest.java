package cm.gimac.des.document;

import cm.gimac.des.document.config.AsyncSyncConfiguration;
import cm.gimac.des.document.config.EmbeddedKafka;
import cm.gimac.des.document.config.EmbeddedRedis;
import cm.gimac.des.document.config.EmbeddedSQL;
import cm.gimac.des.document.config.JacksonConfiguration;
import cm.gimac.des.document.config.TestSecurityConfiguration;
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
@SpringBootTest(classes = { DocumentApp.class, JacksonConfiguration.class, AsyncSyncConfiguration.class, TestSecurityConfiguration.class })
@EmbeddedRedis
@EmbeddedSQL
@EmbeddedKafka
public @interface IntegrationTest {
}
