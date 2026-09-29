package cm.gimac.des.iam.cucumber;

import cm.gimac.des.iam.AbstractIamIT;
import io.cucumber.spring.CucumberContextConfiguration;

/** Contexte des scénarios Gherkin : mêmes classes que AbstractIamIT, contexte Spring partagé. */
@CucumberContextConfiguration
public class CucumberTestContextConfiguration extends AbstractIamIT {}
