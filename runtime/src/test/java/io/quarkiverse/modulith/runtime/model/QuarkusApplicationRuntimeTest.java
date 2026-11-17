package io.quarkiverse.modulith.runtime.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class QuarkusApplicationRuntimeTest {

    static final QuarkusApplicationRuntime RUNTIME = new QuarkusApplicationRuntime("shop", Bean.class.getName(),
            List.of("io.quarkiverse.modulith.runtime"));
    static final QuarkusApplicationRuntime UNNAMED = new QuarkusApplicationRuntime("", "", List.of());

    @ParameterizedTest
    @MethodSource("beans")
    void stripsGeneratedClasses(Object bean) {
        // act
        Class<?> userClass = RUNTIME.getUserClass(bean, "bean");

        // assert
        assertThat(userClass).isEqualTo(Bean.class);
    }

    static List<Object> beans() {
        return List.of(new Bean(), new Bean_Subclass(), new Bean_Subclass_ClientProxy());
    }

    @ParameterizedTest
    @MethodSource("classes")
    void tellsApplicationClassesByTheirRootPackage(Class<?> type, boolean expected) {
        // act
        boolean application = RUNTIME.isApplicationClass(type);

        // assert
        assertThat(application).isEqualTo(expected);
    }

    static List<Object[]> classes() {
        return List.of(new Object[] { Bean.class, true }, new Object[] { String.class, false });
    }

    @ParameterizedTest
    @MethodSource("ids")
    void hasTheApplicationNameAsId(QuarkusApplicationRuntime runtime, Optional<String> expected) {
        // act
        Optional<String> id = runtime.getId();

        // assert
        assertThat(id).isEqualTo(expected);
    }

    static List<Object[]> ids() {
        return List.of(new Object[] { RUNTIME, Optional.of("shop") }, new Object[] { UNNAMED, Optional.empty() });
    }

    @ParameterizedTest
    @MethodSource("mainClasses")
    void loadsTheMainApplicationClass(QuarkusApplicationRuntime runtime, Optional<Class<?>> expected) {
        // act
        Optional<Class<?>> mainClass = runtime.getMainApplicationClass();

        // assert
        assertThat(mainClass).isEqualTo(expected);
    }

    static List<Object[]> mainClasses() {
        return List.of(new Object[] { RUNTIME, Optional.of(Bean.class) }, new Object[] { UNNAMED, Optional.empty() });
    }

    @Test
    void failsForAMainApplicationClassThatCannotBeLoaded() {
        // arrange
        QuarkusApplicationRuntime runtime = new QuarkusApplicationRuntime("", "com.acme.Missing", List.of());

        // act & assert
        assertThatThrownBy(runtime::getMainApplicationClass)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot load the application class com.acme.Missing");
    }
}
