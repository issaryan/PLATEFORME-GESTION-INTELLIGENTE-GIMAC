package cm.gimac.des.rapprochement;

import cm.gimac.des.rapprochement.config.AsyncSyncConfiguration;
import cm.gimac.des.rapprochement.config.EmbeddedKafka;
import cm.gimac.des.rapprochement.config.EmbeddedRedis;
import cm.gimac.des.rapprochement.config.EmbeddedSQL;
import cm.gimac.des.rapprochement.config.JacksonConfiguration;
import cm.gimac.des.rapprochement.config.TestSecurityConfiguration;
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
    classes = { RapprochementApp.class, JacksonConfiguration.class, AsyncSyncConfiguration.class, TestSecurityConfiguration.class }
)
@EmbeddedRedis
@EmbeddedSQL
@EmbeddedKafka
public @interface IntegrationTest {
}
