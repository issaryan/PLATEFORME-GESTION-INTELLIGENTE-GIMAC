package cm.gimac.des.homologation.cucumber;

import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@IncludeEngines("cucumber")
@SelectPackages("cm.gimac.des.homologation.cucumber")
class CucumberTest {}
