package io.quarkiverse.modulith.deployment.docs;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.deployment.events.ListenerDeclarations;
import io.quarkiverse.modulith.deployment.model.ModuleDescriptor;
import io.quarkiverse.modulith.deployment.model.ModuleModel;
import io.quarkiverse.modulith.deployment.model.ModuleNamedInterface;
import io.quarkiverse.modulith.docs.CoreModels;
import io.quarkiverse.modulith.docs.DocumentationModel;
import io.quarkiverse.modulith.docs.ListenerMethod;
import io.quarkiverse.modulith.docs.ModuleBean;
import io.quarkiverse.modulith.docs.ModuleDetails;
import io.quarkiverse.modulith.docs.ModuleProperty;
import io.quarkiverse.modulith.docs.ModuleType;
import io.quarkiverse.modulith.docs.Stereotype;
import io.quarkiverse.modulith.docs.metadata.DocumentationSource;

/**
 * Builds the {@link DocumentationModel} of the application from the module model and the Jandex index: types with
 * annotations and supertypes, beans with stereotypes and listener methods, typed dependencies, events with the
 * methods creating them, aggregate roots, value types, configuration properties and initializers.
 */
public final class DocumentationModelBuilder {

    static final String UNNAMED_INTERFACE = "<<UNNAMED>>";

    private final TypeInspector types;
    private final Map<DotName, AnnotationInstance> listenerAnnotations;

    public DocumentationModelBuilder(IndexView index) {
        this.types = new TypeInspector(index);
        this.listenerAnnotations = ListenerDeclarations.annotationTypes(index);
    }

    public DocumentationModel build(ModuleModel model, ApplicationModules modules, DocumentationSource javadoc) {
        Set<String> panacheEntities = panacheRepositoryEntities(model);
        List<ModuleDetails> details = model.modules().stream()
                .map(module -> details(modules, module, javadoc, panacheEntities))
                .toList();
        return new DocumentationModel(modules, details, model.sharedModules(), javadoc);
    }

    private ModuleDetails details(ApplicationModules modules, ModuleDescriptor module, DocumentationSource javadoc,
            Set<String> panacheEntities) {
        Map<String, ClassInfo> classes = new TreeMap<>();
        module.classes().stream().filter(it -> !it.endsWith(".package-info"))
                .forEach(name -> types.classInfo(name).ifPresent(info -> classes.put(name, info)));
        Map<String, ModuleType> moduleTypes = new LinkedHashMap<>();
        classes.forEach((name, info) -> moduleTypes.put(name, type(module, info)));

        List<ModuleBean> beans = new ArrayList<>();
        List<ModuleType> aggregates = new ArrayList<>();
        List<ModuleType> valueTypes = new ArrayList<>();
        List<ModuleProperty> properties = new ArrayList<>();
        List<String> initializers = new ArrayList<>();
        ConfigPropertyCollector configProperties = new ConfigPropertyCollector(types, javadoc);
        classes.forEach((name, info) -> {
            ModuleType type = moduleTypes.get(name);
            if (isBean(info, type)) {
                beans.add(bean(module, info, type, moduleTypes));
            }
            if (isAggregateRoot(type, panacheEntities)) {
                aggregates.add(type);
            }
            if (type.isAnnotatedWith(Names.JMOLECULES_VALUE_OBJECT.toString())
                    || type.isAssignableTo("org.jmolecules.ddd.types.ValueObject")
                    || type.isAnnotatedWith(Names.EMBEDDABLE.toString())) {
                valueTypes.add(type);
            }
            if (!info.isInterface() && type.isAssignableTo(Names.APPLICATION_MODULE_INITIALIZER.toString())) {
                initializers.add(name);
            }
            properties.addAll(configProperties.collect(info));
        });
        properties.sort(Comparator.comparing(ModuleProperty::name));

        return new ModuleDetails(module.name(),
                modules.getParentOf(module.applicationModule()).map(ApplicationModule::getName),
                List.copyOf(moduleTypes.values()), beans,
                CoreModels.dependencies(modules, module.applicationModule()),
                CoreModels.events(module.applicationModule()), aggregates, valueTypes, properties,
                namedInterfaces(module), initializers);
    }

    private ModuleType type(ModuleDescriptor module, ClassInfo info) {
        String name = info.name().toString();
        Set<String> annotations = types.annotations(info);
        return new ModuleType(name, Modifier.isPublic(info.flags()), module.exposes(name), annotations,
                types.metaAnnotations(annotations), types.packageAnnotations(name), types.supertypes(info));
    }

    private boolean isBean(ClassInfo info, ModuleType type) {
        if (TypeInspector.hasAnnotation(info, Names.CONFIG_MAPPING)) {
            return true;
        }
        if (info.isInterface() || Modifier.isAbstract(info.flags())) {
            return false;
        }
        boolean beanDefining = Names.BEAN_DEFINING.stream().anyMatch(it -> type.isAnnotatedWith(it.toString()))
                || type.isMetaAnnotatedWith(Names.STEREOTYPE.toString())
                || type.isAnnotatedWith(Names.PATH.toString());
        boolean autoScoped = !listenerMethods(info).isEmpty()
                || TypeInspector.hasMethodAnnotation(info, Names.SCHEDULED)
                || TypeInspector.hasMethodAnnotation(info, Names.INCOMING)
                || TypeInspector.hasMethodAnnotation(info, Names.OUTGOING)
                || TypeInspector.hasMethodAnnotation(info, Names.ROUTE);
        return beanDefining || autoScoped;
    }

    private ModuleBean bean(ModuleDescriptor module, ClassInfo info, ModuleType type, Map<String, ModuleType> types) {
        List<ModuleType> interfaces = type.supertypes().stream()
                .filter(types::containsKey)
                .map(types::get)
                .filter(it -> this.types.classInfo(it.name()).map(ClassInfo::isInterface).orElse(false))
                .toList();
        boolean apiBean = type.exposed() || interfaces.stream().anyMatch(ModuleType::exposed);
        List<ListenerMethod> listeners = listenerMethods(info);
        return new ModuleBean(type, apiBean, interfaces, stereotypes(info, type, listeners), listeners);
    }

    private Set<Stereotype> stereotypes(ClassInfo info, ModuleType type, List<ListenerMethod> listeners) {
        Set<Stereotype> result = EnumSet.noneOf(Stereotype.class);
        if (type.isAnnotatedWith(Names.PATH.toString()) || type.isAnnotatedWith(Names.ROUTE_BASE.toString())
                || TypeInspector.hasMethodAnnotation(info, Names.PATH)
                || TypeInspector.hasMethodAnnotation(info, Names.ROUTE)) {
            result.add(Stereotype.CONTROLLER);
        }
        if (annotatedOrMeta(type, Names.JMOLECULES_SERVICE) || type.name().endsWith("Service")) {
            result.add(Stereotype.SERVICE);
        }
        if (annotatedOrMeta(type, Names.JMOLECULES_REPOSITORY)
                || annotatedOrMeta(type, Names.JAKARTA_DATA_REPOSITORY)
                || Names.REPOSITORY_TYPES.stream().anyMatch(type::isAssignableTo)) {
            result.add(Stereotype.REPOSITORY);
        }
        if (!listeners.isEmpty()) {
            result.add(Stereotype.EVENT_LISTENER);
        }
        if (TypeInspector.hasAnnotation(info, Names.CONFIG_MAPPING) || hasConfigProperty(info)) {
            result.add(Stereotype.CONFIGURATION_PROPERTIES);
        }
        if (TypeInspector.hasMethodAnnotation(info, Names.SCHEDULED)) {
            result.add(Stereotype.SCHEDULED_JOB);
        }
        if (TypeInspector.hasMethodAnnotation(info, Names.INCOMING)
                || TypeInspector.hasMethodAnnotation(info, Names.OUTGOING)) {
            result.add(Stereotype.MESSAGING_ENDPOINT);
        }
        return result;
    }

    private static boolean annotatedOrMeta(ModuleType type, DotName annotation) {
        return type.isAnnotatedWith(annotation.toString()) || type.isMetaAnnotatedWith(annotation.toString());
    }

    private static boolean hasConfigProperty(ClassInfo info) {
        return info.fields().stream().anyMatch(it -> it.hasDeclaredAnnotation(Names.CONFIG_PROPERTY))
                || info.methods().stream()
                        .anyMatch(it -> TypeInspector.hasParameterAnnotation(it, Names.CONFIG_PROPERTY));
    }

    /**
     * @return the event listener methods of the class, ordered by signature
     */
    List<ListenerMethod> listenerMethods(ClassInfo info) {
        List<ListenerMethod> result = new ArrayList<>();
        for (MethodInfo method : info.methods()) {
            if (method.parametersCount() == 0) {
                continue;
            }
            List<String> parameters = method.parameterTypes().stream().map(TypeInspector::typeName).toList();
            boolean springAsync = method.hasDeclaredAnnotation(Names.ASYNC);
            Optional<AnnotationInstance> listener = moduleListener(method);
            if (listener.isPresent()) {
                boolean async = Optional.ofNullable(listener.get().value("async")).map(AnnotationValue::asBoolean)
                        .orElse(true);
                result.add(new ListenerMethod(method.name(), parameters, List.of(parameters.get(0)), async));
            } else if (method.hasDeclaredAnnotation(Names.SPRING_MODULE_LISTENER)) {
                result.add(new ListenerMethod(method.name(), parameters, List.of(parameters.get(0)), true));
            } else if (method.hasDeclaredAnnotation(Names.SPRING_EVENT_LISTENER)) {
                result.add(new ListenerMethod(method.name(), parameters, List.of(parameters.get(0)), springAsync));
            } else {
                for (MethodParameterInfo parameter : method.parameters()) {
                    boolean observes = parameter.hasDeclaredAnnotation(Names.OBSERVES);
                    boolean observesAsync = parameter.hasDeclaredAnnotation(Names.OBSERVES_ASYNC);
                    if (observes || observesAsync) {
                        result.add(new ListenerMethod(method.name(), parameters,
                                List.of(TypeInspector.typeName(parameter.type())), observesAsync || springAsync));
                    }
                }
            }
        }
        result.sort(Comparator.comparing(ListenerMethod::signature));
        return result;
    }

    /**
     * @return the {@code @ApplicationModuleListener} whose attributes apply to the method, declared on the method or
     *         on a composed annotation the method carries
     */
    private Optional<AnnotationInstance> moduleListener(MethodInfo method) {
        return listenerAnnotations.entrySet().stream().filter(it -> method.hasDeclaredAnnotation(it.getKey()))
                .findFirst()
                .map(it -> Objects.requireNonNullElse(it.getValue(), method.declaredAnnotation(it.getKey())));
    }

    private boolean isAggregateRoot(ModuleType type, Set<String> panacheEntities) {
        return annotatedOrMeta(type, Names.JMOLECULES_AGGREGATE_ROOT)
                || type.isAssignableTo("org.jmolecules.ddd.types.AggregateRoot")
                || type.isAnnotatedWith(Names.ENTITY.toString())
                        && (panacheEntities.contains(type.name())
                                || Names.PANACHE_ENTITIES.stream().anyMatch(type::isAssignableTo));
    }

    /**
     * @return the entity types managed by Panache repositories of the application
     */
    private Set<String> panacheRepositoryEntities(ModuleModel model) {
        Set<String> result = new TreeSet<>();
        for (ModuleDescriptor module : model.modules()) {
            for (String name : module.classes()) {
                types.classInfo(name).ifPresent(info -> info.interfaceTypes().stream()
                        .filter(it -> it.kind() == Type.Kind.PARAMETERIZED_TYPE)
                        .filter(it -> it.name().toString().startsWith("io.quarkus.")
                                && it.name().toString().contains("Panache"))
                        .map(it -> it.asParameterizedType().arguments().get(0).name().toString())
                        .forEach(result::add));
            }
        }
        return result;
    }

    private Map<String, List<String>> namedInterfaces(ModuleDescriptor module) {
        Map<String, List<String>> result = new TreeMap<>();
        List<String> classes = module.classes().stream().filter(it -> !it.endsWith(".package-info"))
                .filter(it -> !it.contains("$")).toList();
        result.put(UNNAMED_INTERFACE, classes.stream()
                .filter(it -> module.open() || module.isInBasePackage(it))
                .filter(this::isPublic)
                .toList());
        for (ModuleNamedInterface namedInterface : module.namedInterfaces()) {
            result.put(namedInterface.name(), classes.stream().filter(namedInterface::contains).toList());
        }
        return result;
    }

    private boolean isPublic(String name) {
        return types.classInfo(name).map(it -> Modifier.isPublic(it.flags())).orElse(false);
    }
}
