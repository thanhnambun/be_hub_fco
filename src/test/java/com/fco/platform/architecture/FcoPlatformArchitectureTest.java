package com.fco.platform.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Baseline architecture guardrails for {@code com.fco.platform.common} (production sources only).
 * <p>Expand to full {@code com.fco.platform} once legacy references (e.g. {@code AlertService} in
 * {@code GlobalExceptionHandler}) are moved out of {@code common.advice}.</p>
 */
class FcoPlatformArchitectureTest {

    private static JavaClasses fcoCommonProductionClasses() {
        return new ClassFileImporter().importPackages("com.fco.platform.common");
    }

    @Test
    void commonSubpackagesShouldBeFreeOfCycles() {
        slices()
                .matching("com.fco.platform.common.(*)..")
                .should()
                .beFreeOfCycles()
                .check(fcoCommonProductionClasses());
    }
}
