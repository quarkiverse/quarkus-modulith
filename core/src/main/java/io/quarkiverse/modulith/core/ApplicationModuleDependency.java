package io.quarkiverse.modulith.core;

/**
 * One reference from a type of a module to a type of another module.
 */
public interface ApplicationModuleDependency {

    /**
     * @return the fully qualified name of the referencing type
     */
    String getSourceType();

    /**
     * @return the fully qualified name of the referenced type
     */
    String getTargetType();

    DependencyType getDependencyType();

    /**
     * @return the module of the referenced type
     */
    ApplicationModule getTargetModule();

    /**
     * @return the referencing type
     */
    JavaType getSourceJavaType();

    /**
     * @return the referenced type
     */
    JavaType getTargetJavaType();

    /**
     * @return where and how the reference is made
     */
    String getDescription();
}
