package cm.gimac.des.monitoring;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packagesOf = MonitoringApp.class, importOptions = DoNotIncludeTests.class)
class TechnicalStructureTest {

    // prettier-ignore
    // Couches générées par JHipster, bornées au paquet racine, et couches hexagonales de la supervision et des astreintes
    // (PI-GIMAC-01 §0.8, D-114) : application (cas d'utilisation, ports) et infrastructure (adaptateurs).
    @ArchTest
    static final ArchRule respectsTechnicalArchitectureLayers = layeredArchitecture()
        .consideringAllDependencies()
        .layer("Config").definedBy("cm.gimac.des.monitoring.config..")
        .layer("Web").definedBy("cm.gimac.des.monitoring.web..")
        .optionalLayer("Service").definedBy("cm.gimac.des.monitoring.service..")
        .layer("Security").definedBy("cm.gimac.des.monitoring.security..")
        .optionalLayer("Persistence").definedBy("cm.gimac.des.monitoring.repository..")
        .layer("Domain").definedBy("cm.gimac.des.monitoring.domain..")
        .optionalLayer("Application").definedBy("cm.gimac.des.monitoring.application..")
        .layer("Infrastructure").definedBy("cm.gimac.des.monitoring.infrastructure..")

        .whereLayer("Config").mayNotBeAccessedByAnyLayer()
        .whereLayer("Web").mayOnlyBeAccessedByLayers("Config")
        .whereLayer("Service").mayOnlyBeAccessedByLayers("Web", "Config", "Application")
        .whereLayer("Security").mayOnlyBeAccessedByLayers("Config", "Service", "Web", "Application", "Infrastructure")
        .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Service", "Security", "Web", "Config", "Application", "Infrastructure")
        .whereLayer("Domain").mayOnlyBeAccessedByLayers("Persistence", "Service", "Security", "Web", "Config", "Application", "Infrastructure")
        .whereLayer("Application").mayOnlyBeAccessedByLayers("Web", "Config", "Infrastructure")
        .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("Config", "Web", "Service", "Application")

        .ignoreDependency(belongToAnyOf(MonitoringApp.class), alwaysTrue())
        .ignoreDependency(alwaysTrue(), belongToAnyOf(
            cm.gimac.des.monitoring.config.Constants.class,
            cm.gimac.des.monitoring.config.ApplicationProperties.class
        ));

    // Services de domaine purs (REC-GIMAC-01 §6 point 1) : ni Spring, ni infrastructure, ni web.
    @ArchTest
    static final ArchRule domainServicesArePure = com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses()
        .that().resideInAPackage("cm.gimac.des.monitoring.domain.service..")
        .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "cm.gimac.des.monitoring.infrastructure..", "cm.gimac.des.monitoring.web..", "cm.gimac.des.monitoring.application..")
        .allowEmptyShould(true);
}
