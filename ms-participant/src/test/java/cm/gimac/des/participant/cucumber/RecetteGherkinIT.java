package cm.gimac.des.participant.cucumber;

import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/** Recette : scénarios Gherkin du carnet de produit, exécutés au « verify » avec Testcontainers (D-61). */
@Suite
@IncludeEngines("cucumber")
@SelectPackages("cm.gimac.des.participant.cucumber")
class RecetteGherkinIT {}
