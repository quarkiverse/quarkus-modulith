package io.quarkiverse.modulith.docs;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.DependencyType;

/**
 * Renders the parts of the module canvases and of the aggregating document as Asciidoc, like Spring Modulith's
 * {@code Asciidoctor}.
 */
final class Asciidoctor {

    /**
     * The line separator of the Asciidoc files, the system one as in Spring Modulith, while the PlantUML files always use
     * {@code \n} as Structurizr's exporter does.
     */
    static final String NL = System.lineSeparator();

    private static final Pattern JAVADOC_CODE = Pattern.compile("\\{\\@(link|code|literal)\\s*(.*?)\\}");
    private static final Pattern LINE_BREAKS = Pattern.compile("\\<\\s*br\\s*\\>");

    private final DocumentationModel model;
    private final Optional<String> apiBase;

    Asciidoctor(DocumentationModel model, Optional<String> apiBase) {
        this.model = model;
        this.apiBase = apiBase;
    }

    /**
     * @param source a type name, optionally followed by {@code #method}
     */
    String toInlineCode(String source) {
        String[] parts = source.split("#");
        String type = parts[0];
        Optional<String> method = parts.length == 2 ? Optional.of(parts[1]) : Optional.empty();
        if (type.isBlank()) {
            return method.map(Asciidoctor::toCode).orElse(source);
        }
        return model.type(type).map(it -> toOptionalLink(it, method)).orElseGet(() -> toCode(type));
    }

    String toInlineCode(ModuleBean bean) {
        String base = withDocumentation(toOptionalLink(bean.type(), Optional.empty()), bean.type().name());
        if (bean.interfacesWithinModule().isEmpty()) {
            return base;
        }
        String interfaces = bean.interfacesWithinModule().stream()
                .map(it -> toOptionalLink(it, Optional.empty()))
                .collect(Collectors.joining(", "));
        return "%s (via %s)".formatted(interfaces, base);
    }

    String renderBeans(ModuleDetails module, CanvasOptions options) {
        Map<Grouping, List<ModuleBean>> groupings = options.groupBeans(module);
        if (groupings.size() == 1 && groupings.containsKey(CanvasOptions.FALLBACK_GROUP)) {
            return beansToBulletPoints(groupings.get(CanvasOptions.FALLBACK_GROUP));
        }
        StringBuilder builder = new StringBuilder();
        groupings.forEach((grouping, beans) -> {
            if (!builder.isEmpty()) {
                builder.append(NL).append(NL);
            }
            builder.append('_').append(grouping.getName()).append('_');
            grouping.getDescription().ifPresent(it -> builder.append(" -- ").append(it));
            builder.append(NL).append(NL).append(beansToBulletPoints(beans));
        });
        return builder.isEmpty() ? "None" : builder.toString();
    }

    String renderBeanReferences(ApplicationModule module) {
        String bullets = model.details(module).dependencies().stream()
                .filter(it -> it.type() == DependencyType.USES_COMPONENT)
                .collect(Collectors.toMap(ModuleDependency::targetType, it -> it, (first, second) -> first,
                        LinkedHashMap::new))
                .values().stream()
                .map(it -> {
                    ModuleType target = model.type(it.targetType()).orElseGet(() -> ModuleType.of(it.targetType()));
                    String targetModule = model.modules().getModuleByName(it.targetModule())
                            .map(ApplicationModule::getDisplayName).orElse(it.targetModule());
                    String result = "%s (in %s)".formatted(toOptionalLink(target, Optional.empty()), targetModule);
                    return toBulletPoint(withDocumentation(result, target.name()));
                })
                .collect(Collectors.joining(NL));
        return bullets.isBlank() ? "None" : bullets;
    }

    String renderPublishedEvents(ApplicationModule module) {
        List<PublishedEvent> events = model.details(module).publishedEvents();
        if (events.isEmpty()) {
            return "none";
        }
        StringBuilder builder = new StringBuilder();
        for (PublishedEvent event : events) {
            if (!event.type().exposed()) {
                continue;
            }
            Optional<String> documentation = documentation(event.type().name()).map(" -- "::concat);
            builder.append("* ").append(toOptionalLink(event.type(), Optional.empty()));
            documentation.ifPresent(builder::append);
            if (event.sources().isEmpty()) {
                builder.append(NL);
            } else {
                builder.append(documentation.isPresent() ? " C" : " c").append("reated by:").append(NL);
            }
            for (EventSource source : event.sources()) {
                String text = "%s.%s(%s)".formatted(TypeNames.abbreviate(source.type(), module.getBasePackage()),
                        source.method(), source.parameters() ? "…" : "");
                builder.append("** ").append(toInlineCode(text)).append(NL);
            }
        }
        return builder.toString();
    }

    /**
     * A listener method without an exposed event type renders as an empty line, as in Spring Modulith.
     */
    String renderEventsListenedTo(ApplicationModule module) {
        return model.details(module).beans().stream()
                .filter(bean -> bean.is(Stereotype.EVENT_LISTENER))
                .flatMap(bean -> bean.listenerMethods().stream().map(method -> renderListenerMethod(bean, method)))
                .collect(Collectors.joining(NL));
    }

    private String renderListenerMethod(ModuleBean bean, ListenerMethod method) {
        List<String> exposed = method.referenceTypes().stream().filter(model::isExposed).toList();
        if (exposed.isEmpty()) {
            return "";
        }
        String types = exposed.stream().map(this::toInlineCode).collect(Collectors.joining(", "));
        String async = method.async() ? "(async) " : "";
        return model.documentation().getDocumentation(bean.type().name(), method.signature())
                .map(this::toAsciidoctor)
                .map(doc -> "* %s %s-- %s".formatted(types, async, doc))
                .orElseGet(() -> "* %s %s".formatted(types, async));
    }

    String renderConfigurationProperties(List<ModuleProperty> properties) {
        if (properties.isEmpty()) {
            return "none";
        }
        return toBulletPoints(properties.stream().map(property -> {
            StringBuilder builder = new StringBuilder(toCode(property.name())).append(" -- ")
                    .append(toInlineCode(property.type()));
            property.defaultValue().filter(it -> !it.isBlank())
                    .ifPresent(it -> builder.append(", default ").append(toInlineCode(it)));
            property.description().filter(it -> !it.isBlank())
                    .ifPresent(it -> builder.append(". ").append(toAsciidoctor(it)));
            return builder.toString();
        }));
    }

    String typesToBulletPoints(List<ModuleType> types) {
        return toBulletPoints(types.stream()
                .map(it -> withDocumentation(toOptionalLink(it, Optional.empty()), it.name())));
    }

    String renderModuleDescription(ApplicationModule module) {
        return documentation(module.getBasePackage()).orElse("");
    }

    String toAsciidoctor(String source) {
        String result = source;
        Matcher matcher = JAVADOC_CODE.matcher(source);
        while (matcher.find()) {
            result = result.replace(matcher.group(), toInlineCode(matcher.group(2)));
        }
        result = result.replaceAll("<p>\\s*", Matcher.quoteReplacement(NL + "+" + NL));
        result = result.replace("</p>", "");
        return LINE_BREAKS.matcher(result).replaceAll(Matcher.quoteReplacement(NL));
    }

    static String renderHeadline(int level, String text) {
        return "=".repeat(level) + " " + text + NL;
    }

    static String renderPlantUmlInclude(String file) {
        return "plantuml::" + file + "[,,format=svg]" + NL;
    }

    static String renderGeneralInclude(String file) {
        return "include::" + file + "[]" + NL;
    }

    static String startTable(String tableSpec) {
        return "[" + tableSpec + "]" + NL + "|===" + NL;
    }

    static String startOrEndTable() {
        return "|===" + NL;
    }

    static String writeTableRow(String... columns) {
        return Stream.of(columns).collect(Collectors.joining(NL + "|", "|", NL));
    }

    static String toCode(String source) {
        return "`" + source + "`";
    }

    String toBulletPoint(String source) {
        return "* " + source;
    }

    private String beansToBulletPoints(List<ModuleBean> beans) {
        return toBulletPoints(beans.stream().map(this::toInlineCode));
    }

    private static String toBulletPoints(Stream<String> items) {
        return items.collect(Collectors.joining(NL + "* ", "* ", ""));
    }

    private Optional<String> documentation(String typeOrPackage) {
        return model.documentation().getDocumentation(typeOrPackage).map(this::toAsciidoctor);
    }

    private String withDocumentation(String base, String type) {
        return documentation(type).map(it -> base + " -- " + it).orElse(base);
    }

    private String toOptionalLink(ModuleType type, Optional<String> method) {
        Optional<ApplicationModule> module = model.moduleByType(type.name());
        String formatted = TypeNames.abbreviate(type.name(), module.map(ApplicationModule::getBasePackage).orElse(null));
        String typeAndMethod = toCode(method.map(it -> formatted + "#" + it).orElse(formatted));
        if (module.isEmpty() || !type.publicType()) {
            return typeAndMethod;
        }
        String classPath = type.name().replace('.', '/').replace('$', '.');
        return apiBase.map(base -> "link:%s/%s.html[%s]".formatted(base, classPath, typeAndMethod))
                .orElse(typeAndMethod);
    }
}
