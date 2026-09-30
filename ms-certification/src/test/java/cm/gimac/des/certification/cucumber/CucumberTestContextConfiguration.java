package cm.gimac.des.certification.cucumber;

import cm.gimac.des.certification.AbstractCertificationIT;
import cm.gimac.des.certification.security.AuthoritiesConstants;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * Contexte des scénarios Gherkin : mêmes classes que AbstractCertificationIT (Kafka compris), pour
 * partager le contexte Spring et vérifier l'émission des événements.
 */
@CucumberContextConfiguration
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
public class CucumberTestContextConfiguration extends AbstractCertificationIT {}
