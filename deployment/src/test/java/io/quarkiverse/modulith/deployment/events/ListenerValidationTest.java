package io.quarkiverse.modulith.deployment.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.deployment.Failures;
import io.quarkiverse.modulith.deployment.fixtures.events.invalid.order.NotABean;
import io.quarkus.test.QuarkusExtensionTest;

public class ListenerValidationTest {

    static final String ORDER = "io.quarkiverse.modulith.deployment.fixtures.events.invalid.order.";
    static final AtomicReference<Throwable> FAILURE = new AtomicReference<>();

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, "io.quarkiverse.modulith.deployment.fixtures.events.invalid"))
            .overrideConfigKey("quarkus.modulith.base-package", "io.quarkiverse.modulith.deployment.fixtures.events.invalid")
            .overrideConfigKey("quarkus.arc.exclude-types", NotABean.class.getName())
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .assertException(FAILURE::set);

    @Test
    void reportsEveryInvalidListenerInOneFailure() {
        // arrange
        String expectedConditionPrefix = "Invalid @ApplicationModuleListener condition \"";

        // act
        String messages = Failures.messages(FAILURE.get());

        // assert
        assertThat(messages)
                .contains("@ApplicationModuleListener method must not be static or private: " + ORDER + "StaticListener#on")
                .contains("@ApplicationModuleListener method must not be static or private: " + ORDER + "PrivateListener#on")
                .containsAnyOf("Duplicate @ApplicationModuleListener id dup on " + ORDER + "DuplicateListener#first and "
                        + ORDER + "DuplicateListener#second: give the listeners distinct ids",
                        "Duplicate @ApplicationModuleListener id dup on " + ORDER + "DuplicateListener#second and "
                                + ORDER + "DuplicateListener#first: give the listeners distinct ids")
                .contains("@ApplicationModuleListener method is not declared on a bean: " + ORDER + "NotABean#on")
                .contains("@ApplicationModuleListener propagation MANDATORY cannot work, listeners run after the"
                        + " publishing transaction completed: " + ORDER + "UnsupportedAttributesListener#mandatory")
                .contains(expectedConditionPrefix + "#event.missing > 1\" on " + ORDER
                        + "UnsupportedAttributesListener#unknownProperty: No public method getMissing(), isMissing(),"
                        + " missing() without parameters on " + ORDER + "Amount for 'missing'")
                .contains(expectedConditionPrefix + "#event.value >\" on " + ORDER
                        + "UnsupportedAttributesListener#incomplete: Unexpected end of the condition")
                .contains(expectedConditionPrefix + "#event.value > 1.2.3\" on " + ORDER
                        + "UnsupportedAttributesListener#invalidNumber: Invalid number '1.2.3'")
                .contains(expectedConditionPrefix + "#event.value().signum() > 1\" on " + ORDER
                        + "UnsupportedAttributesListener#primitiveHasNoMembers: Cannot read 'signum()' of long: the"
                        + " type is not a class of the application index")
                .contains(expectedConditionPrefix + "#event.tags.length > 0\" on " + ORDER
                        + "UnsupportedAttributesListener#arrayHasNoMembers: Cannot read 'length' of")
                .doesNotContain("BlankIdListener", "CustomListener", "defaultsSpelledOut");
    }
}
