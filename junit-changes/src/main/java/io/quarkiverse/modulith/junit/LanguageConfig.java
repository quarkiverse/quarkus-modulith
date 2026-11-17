package io.quarkiverse.modulith.junit;

import java.util.function.Function;

import io.quarkiverse.modulith.junit.diff.ModifiedFile;

/**
 * The source directory layout of a JVM language and the changes created for its main and test sources.
 *
 * @param mainDir the main source directory
 * @param testDir the test source directory
 * @param mainFactory creates the change of a main source file from the class name
 * @param testFactory creates the change of a test source file from the class name
 */
record LanguageConfig(String mainDir, String testDir, Function<String, SourceChange> mainFactory,
        Function<String, SourceChange> testFactory) {

    static final LanguageConfig JAVA = new LanguageConfig("src/main/java", "src/test/java", JavaSourceChange::new,
            JavaTestSourceChange::new);

    static final LanguageConfig KOTLIN = new LanguageConfig("src/main/kotlin", "src/test/kotlin", KotlinSourceChange::new,
            KotlinTestSourceChange::new);

    /**
     * Derives the class name from the path below the source directory found first, or from the whole path if the
     * file is not located in a standard source directory.
     */
    SourceChange toChange(ModifiedFile file) {
        var withoutExtension = file.path().substring(0, file.path().lastIndexOf('.'));
        var startOfMainDir = withoutExtension.indexOf(mainDir);
        var startOfTestDir = withoutExtension.indexOf(testDir);
        if (startOfTestDir > -1 && (startOfMainDir < 0 || startOfTestDir < startOfMainDir)) {
            return testFactory.apply(toClassName(withoutExtension.substring(startOfTestDir + testDir.length() + 1)));
        }
        if (startOfMainDir > -1) {
            return mainFactory.apply(toClassName(withoutExtension.substring(startOfMainDir + mainDir.length() + 1)));
        }
        return mainFactory.apply(toClassName(withoutExtension));
    }

    private static String toClassName(String resourcePath) {
        return resourcePath.replace('/', '.');
    }
}
