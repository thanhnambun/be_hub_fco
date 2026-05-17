package com.fco.platform;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "com.fco.platform", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    @ArchTest
    static final ArchRule layer_dependencies_are_respected = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Interfaces").definedBy(
                    "com.fco.platform.auth.interfaces..",
                    "com.fco.platform.player.interfaces..",
                    "com.fco.platform.card.interfaces..",
                    "com.fco.platform.sync.interfaces.."
            )
            .layer("Application").definedBy(
                    "com.fco.platform.auth.application..",
                    "com.fco.platform.player.application..",
                    "com.fco.platform.card.application..",
                    "com.fco.platform.sync.application.."
            )
            .layer("Domain").definedBy(
                    "com.fco.platform.auth.domain..",
                    "com.fco.platform.player.domain..",
                    "com.fco.platform.card.domain..",
                    "com.fco.platform.sync.domain.."
            )
            .layer("Infrastructure").definedBy(
                    "com.fco.platform.auth.infrastructure..",
                    "com.fco.platform.player.infrastructure..",
                    "com.fco.platform.card.infrastructure..",
                    "com.fco.platform.sync.infrastructure.."
            )

            .whereLayer("Interfaces").mayOnlyBeAccessedByLayers("Application")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Interfaces")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Interfaces")
            .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("Application", "Domain");

    @ArchTest
    static final ArchRule controllers_should_not_access_repositories = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Web").definedBy(
                    "com.fco.platform.auth.interfaces.web..",
                    "com.fco.platform.player.interfaces.web..",
                    "com.fco.platform.card.interfaces.web..",
                    "com.fco.platform.sync.interfaces.web.."
            )
            .layer("Repositories").definedBy(
                    "com.fco.platform.auth.infrastructure.persistence..",
                    "com.fco.platform.player.infrastructure.persistence..",
                    "com.fco.platform.card.infrastructure.persistence..",
                    "com.fco.platform.sync.infrastructure.persistence.."
            )
            .layer("Application").definedBy(
                    "com.fco.platform.auth.application..",
                    "com.fco.platform.player.application..",
                    "com.fco.platform.card.application..",
                    "com.fco.platform.sync.application.."
            )
            .layer("Infrastructure").definedBy(
                    "com.fco.platform.auth.infrastructure..",
                    "com.fco.platform.player.infrastructure..",
                    "com.fco.platform.card.infrastructure..",
                    "com.fco.platform.sync.infrastructure.."
            )
            .whereLayer("Repositories").mayOnlyBeAccessedByLayers("Application", "Infrastructure");

    @ArchTest
    static final ArchRule common_should_not_depend_on_other_packages = noClasses()
            .that()
            .resideInAPackage("..common..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..auth..", "..player..", "..card..", "..sync..");
}
