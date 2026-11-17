package io.quarkiverse.modulith.core;

/**
 * How {@link FormattableType#getAbbreviatedFullName(ApplicationModule, NonModuleTypeAbbreviation)} renders a type
 * outside the given module.
 */
public enum NonModuleTypeAbbreviation {

    /**
     * Render the full name.
     */
    FULL_NAME,

    /**
     * Abbreviate the package name.
     */
    ABBREVIATED
}
