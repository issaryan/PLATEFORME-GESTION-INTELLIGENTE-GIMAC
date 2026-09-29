package cm.gimac.des.homologation;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packagesOf = HomologationApp.class, importOptions = DoNotIncludeTests.class)
class TechnicalStructureTest {

    // prettier-ignore
    // Couches générées par JHipster, bornées au paquet racine, et couches hexagonales du lot 1
    // (PI-GIMAC-01 §0.8) : application (cas d'utilisation, ports) et infrastructure (adaptateurs).
    @ArchTest
    static final ArchRule respectsTechnicalArchitectureLayers = layeredArchitecture()
        .consideringAllDependencies()
        .layer("Config").definedBy("cm.gimac.des.homologation.config..")
        .layer("Web").definedBy("cm.gimac.des.homologation.web..")
        .optionalLayer("Service").definedBy("cm.gimac.des.homologation.service..")
        .layer("Security").definedBy("cm.gimac.des.homologation.security..")
        .optionalLayer("Persistence").definedBy("cm.gimac.des.homologation.repository..")
        .layer("Domain").definedBy("cm.gimac.des.homologation.domain..")
        .optionalLayer("Application").definedBy("cm.gimac.des.homologation.application..")
        .layer("Infrastructure").definedBy("cm.gimac.des.homologation.infrastructure..")

        .whereLayer("Config").mayNotBeAccessedByAnyLayer()
        .whereLayer("Web").mayOnlyBeAccessedByLayers("Config")
        .whereLayer("Service").mayOnlyBeAccessedByLayers("Web", "Config", "Application")
        .whereLayer("Security").mayOnlyBeAccessedByLayers("Config", "Service", "Web", "Application", "Infrastructure")
        .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Service", "Security", "Web", "Config", "Application", "Infrastructure")
        .whereLayer("Domain").mayOnlyBeAccessedByLayers("Persistence", "Service", "Security", "Web", "Config", "Application", "Infrastructure")
        .whereLayer("Application").mayOnlyBeAccessedByLayers("Web", "Config", "Infrastructure")
        .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("Config", "Web", "Service", "Application")

        .ignoreDependency(belongToAnyOf(HomologationApp.class), alwaysTrue())
        .ignoreDependency(alwaysTrue(), belongToAnyOf(
            cm.gimac.des.homologation.config.Constants.class,
            cm.gimac.des.homologation.config.ApplicationProperties.class
        ));

    // Services de domaine purs (REC-GIMAC-01 §6 point 1) : ni Spring, ni infrastructure, ni web.
    @ArchTest
    static final ArchRule domainServicesArePure = com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses()
        .that().resideInAPackage("cm.gimac.des.homologation.domain.service..")
        .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "cm.gimac.des.homologation.infrastructure..", "cm.gimac.des.homologation.web..", "cm.gimac.des.homologation.application..")
        .allowEmptyShould(true);
}
