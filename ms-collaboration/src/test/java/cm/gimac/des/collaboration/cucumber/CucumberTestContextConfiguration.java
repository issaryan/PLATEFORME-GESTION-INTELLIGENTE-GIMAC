package cm.gimac.des.collaboration.cucumber;

import cm.gimac.des.collaboration.CollaborationApp;
import cm.gimac.des.collaboration.config.AsyncSyncConfiguration;
import cm.gimac.des.collaboration.config.DatabaseTestcontainer;
import cm.gimac.des.collaboration.config.JacksonHibernateConfiguration;
import cm.gimac.des.collaboration.config.KafkaTestContainer;
import cm.gimac.des.collaboration.config.TestSecurityConfiguration;
import cm.gimac.des.collaboration.security.AuthoritiesConstants;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * Contexte des scénarios Gherkin : mêmes classes que AbstractCollaborationIT (Kafka compris), pour
 * partager le contexte Spring et vérifier l'émission des événements du lot 1.
 */
@CucumberContextConfiguration
@SpringBootTest(
    classes = {
        CollaborationApp.class,
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
