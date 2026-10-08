package cm.gimac.des.audit;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packagesOf = AuditApp.class, importOptions = DoNotIncludeTests.class)
class TechnicalStructureTest {

    // prettier-ignore
    // Couches générées par JHipster, bornées au paquet racine, et couches hexagonales du registre (D-93 §11) :
    // application (cas d'utilisation, ports) et infrastructure (adaptateurs, consommateurs Kafka).
    @ArchTest
    static final ArchRule respectsTechnicalArchitectureLayers = layeredArchitecture()
        .consideringAllDependencies()
        .layer("Config").definedBy("cm.gimac.des.audit.config..")
        .layer("Web").definedBy("cm.gimac.des.audit.web..")
        .optionalLayer("Service").definedBy("cm.gimac.des.audit.service..")
        .layer("Security").definedBy("cm.gimac.des.audit.security..")
        .optionalLayer("Persistence").definedBy("cm.gimac.des.audit.repository..")
        .layer("Domain").definedBy("cm.gimac.des.audit.domain..")
        .optionalLayer("Application").definedBy("cm.gimac.des.audit.application..")
        .optionalLayer("Infrastructure").definedBy("cm.gimac.des.audit.infrastructure..")

        .whereLayer("Config").mayNotBeAccessedByAnyLayer()
        .whereLayer("Web").mayOnlyBeAccessedByLayers("Config")
        .whereLayer("Service").mayOnlyBeAccessedByLayers("Web", "Config", "Application")
        .whereLayer("Security").mayOnlyBeAccessedByLayers("Config", "Service", "Web", "Application", "Infrastructure")
        .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Service", "Security", "Web", "Config", "Application")
        .whereLayer("Domain").mayOnlyBeAccessedByLayers("Persistence", "Service", "Security", "Web", "Config", "Application", "Infrastructure")
        .whereLayer("Application").mayOnlyBeAccessedByLayers("Web", "Config", "Infrastructure")
        .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("Config", "Web", "Service", "Application")

        .ignoreDependency(belongToAnyOf(AuditApp.class), alwaysTrue())
        .ignoreDependency(alwaysTrue(), belongToAnyOf(
            cm.gimac.des.audit.config.Constants.class,
            cm.gimac.des.audit.config.ApplicationProperties.class
        ));

    // Arbre de Merkle et natures de signature (domaine du registre) : ni Spring, ni infrastructure, ni web.
    @ArchTest
    static final ArchRule registreEstPur = com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses()
        .that().resideInAPackage("cm.gimac.des.audit.domain.registre..")
        .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "cm.gimac.des.audit.infrastructure..", "cm.gimac.des.audit.web..", "cm.gimac.des.audit.application..");
}
