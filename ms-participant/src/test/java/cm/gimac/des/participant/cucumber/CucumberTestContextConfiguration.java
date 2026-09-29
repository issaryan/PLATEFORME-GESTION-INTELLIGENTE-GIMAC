package cm.gimac.des.participant.cucumber;

import cm.gimac.des.participant.AbstractParticipantIT;
import cm.gimac.des.participant.security.AuthoritiesConstants;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * Contexte des scénarios Gherkin : mêmes classes que AbstractParticipantIT (Kafka compris), pour
 * partager le contexte Spring et vérifier l'émission des événements.
 */
@CucumberContextConfiguration
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
public class CucumberTestContextConfiguration extends AbstractParticipantIT {}
