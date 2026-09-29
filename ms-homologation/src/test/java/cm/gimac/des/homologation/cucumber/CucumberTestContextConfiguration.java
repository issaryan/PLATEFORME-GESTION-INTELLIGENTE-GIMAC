package cm.gimac.des.homologation.cucumber;

import cm.gimac.des.homologation.HomologationApp;
import cm.gimac.des.homologation.config.AsyncSyncConfiguration;
import cm.gimac.des.homologation.config.DatabaseTestcontainer;
import cm.gimac.des.homologation.config.JacksonHibernateConfiguration;
import cm.gimac.des.homologation.config.KafkaTestContainer;
import cm.gimac.des.homologation.config.TestSecurityConfiguration;
import cm.gimac.des.homologation.security.AuthoritiesConstants;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * Contexte des scénarios Gherkin : mêmes classes que AbstractHomologationIT (Kafka compris), pour
 * partager le contexte Spring et vérifier l'émission des événements du lot 1.
 */
@CucumberContextConfiguration
@SpringBootTest(
    classes = {
        HomologationApp.class,
        AsyncSyncConfiguration.class,
        TestSecurityConfiguration.class,
        JacksonHibernateConfiguration.class,
        DatabaseTestcontainer.class,
        KafkaTestContainer.class,
    }
)
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
public class CucumberTestContextConfiguration {}
