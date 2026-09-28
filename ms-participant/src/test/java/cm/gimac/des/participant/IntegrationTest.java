package cm.gimac.des.participant;

import cm.gimac.des.participant.config.AsyncSyncConfiguration;
import cm.gimac.des.participant.config.EmbeddedKafka;
import cm.gimac.des.participant.config.EmbeddedRedis;
import cm.gimac.des.participant.config.EmbeddedSQL;
import cm.gimac.des.participant.config.JacksonConfiguration;
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
    classes = { ParticipantApp.class, JacksonConfiguration.class, AsyncSyncConfiguration.class, TestSecurityConfiguration.class }
)
@EmbeddedRedis
@EmbeddedSQL
@EmbeddedKafka
public @interface IntegrationTest {
}
