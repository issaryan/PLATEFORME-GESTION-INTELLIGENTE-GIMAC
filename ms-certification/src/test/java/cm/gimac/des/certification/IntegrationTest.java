package cm.gimac.des.certification;

import cm.gimac.des.certification.config.AsyncSyncConfiguration;
import cm.gimac.des.certification.config.EmbeddedKafka;
import cm.gimac.des.certification.config.EmbeddedRedis;
import cm.gimac.des.certification.config.EmbeddedSQL;
import cm.gimac.des.certification.config.JacksonConfiguration;
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
    classes = { CertificationApp.class, JacksonConfiguration.class, AsyncSyncConfiguration.class, TestSecurityConfiguration.class }
)
@EmbeddedRedis
@EmbeddedSQL
@EmbeddedKafka
public @interface IntegrationTest {
}
