package cm.gimac.des.iam.cucumber;

import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/** Recette : scénarios Gherkin de l'administration des comptes et du premier accès (D-61, D-62). */
@Suite
@IncludeEngines("cucumber")
@SelectPackages("cm.gimac.des.iam.cucumber")
class RecetteGherkinIT {}
