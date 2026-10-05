package cm.gimac.des.notification;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packagesOf = NotificationApp.class, importOptions = DoNotIncludeTests.class)
class TechnicalStructureTest {

    // prettier-ignore
    // Couches générées par JHipster, bornées au paquet racine, et couches hexagonales des notifications
    // (PI-GIMAC-01 §0.8, D-80) : application (cas d'utilisation, ports) et infrastructure (adaptateurs).
    @ArchTest
    static final ArchRule respectsTechnicalArchitectureLayers = layeredArchitecture()
        .consideringAllDependencies()
        .layer("Config").definedBy("cm.gimac.des.notification.config..")
        .layer("Web").definedBy("cm.gimac.des.notification.web..")
        .optionalLayer("Service").definedBy("cm.gimac.des.notification.service..")
        .layer("Security").definedBy("cm.gimac.des.notification.security..")
        .optionalLayer("Persistence").definedBy("cm.gimac.des.notification.repository..")
        .layer("Domain").definedBy("cm.gimac.des.notification.domain..")
        .optionalLayer("Application").definedBy("cm.gimac.des.notification.application..")
        .layer("Infrastructure").definedBy("cm.gimac.des.notification.infrastructure..")

        .whereLayer("Config").mayNotBeAccessedByAnyLayer()
        .whereLayer("Web").mayOnlyBeAccessedByLayers("Config")
        .whereLayer("Service").mayOnlyBeAccessedByLayers("Web", "Config", "Application")
        .whereLayer("Security").mayOnlyBeAccessedByLayers("Config", "Service", "Web", "Application", "Infrastructure")
        .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Service", "Security", "Web", "Config", "Application", "Infrastructure")
        .whereLayer("Domain").mayOnlyBeAccessedByLayers("Persistence", "Service", "Security", "Web", "Config", "Application", "Infrastructure")
        .whereLayer("Application").mayOnlyBeAccessedByLayers("Web", "Config", "Infrastructure")
        .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("Config", "Web", "Service", "Application")

        .ignoreDependency(belongToAnyOf(NotificationApp.class), alwaysTrue())
        .ignoreDependency(alwaysTrue(), belongToAnyOf(
            cm.gimac.des.notification.config.Constants.class,
            cm.gimac.des.notification.config.ApplicationProperties.class
        ));

    // Services de domaine purs (REC-GIMAC-01 §6 point 1) : ni Spring, ni infrastructure, ni web.
    @ArchTest
    static final ArchRule domainServicesArePure = com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses()
        .that().resideInAPackage("cm.gimac.des.notification.domain.service..")
        .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "cm.gimac.des.notification.infrastructure..", "cm.gimac.des.notification.web..", "cm.gimac.des.notification.application..")
        .allowEmptyShould(true);
}
