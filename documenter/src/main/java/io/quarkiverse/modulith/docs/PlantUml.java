package io.quarkiverse.modulith.docs;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Renders a {@link DiagramView} as PlantUML, producing the same text as the Structurizr C4-PlantUML and PlantUML
 * exporters with the settings Spring Modulith's {@code Documenter} uses.
 */
final class PlantUml {

    private static final String BACKGROUND = "#ffffff";
    private static final String COLOR = "#444444";

    private final StringBuilder out = new StringBuilder();
    private int indent;

    private PlantUml() {
    }

    static String render(DiagramView view, DiagramStyle style, Map<String, String> skinParams) {
        PlantUml writer = new PlantUml();
        return style == DiagramStyle.C4 ? writer.c4(view, skinParams) : writer.uml(view, skinParams);
    }

    private String c4(DiagramView view, Map<String, String> skinParams) {
        DiagramModel model = view.model();
        String system = id(model.systemName());
        List<DiagramComponent> elements = view.elements();
        line("@startuml");
        view.title().ifPresent(title -> line("title <size:24>" + title + "</size>"));
        line();
        line("set separator none");
        line("top to bottom direction");
        skinParams(skinParams);
        line("<style>");
        indent++;
        line("root {");
        indent++;
        line("BackgroundColor: " + BACKGROUND);
        line("FontColor: " + COLOR);
        indent--;
        line("}");
        indent--;
        line("</style>");
        line();
        line("!include <C4/C4>");
        line("!include <C4/C4_Context>");
        if (!elements.isEmpty()) {
            line("!include <C4/C4_Component>");
        }
        line();
        if (!elements.isEmpty()) {
            line("System_Boundary(\"" + system + "_boundary\", \"" + model.systemName() + "\", $tags=\"\") {");
            indent++;
            line("Container_Boundary(\"" + system + "." + system + "_boundary\", \"" + model.systemName()
                    + "\", $tags=\"\") {");
            indent++;
            for (DiagramComponent component : elements) {
                line("Component(" + id(model, component) + ", \"" + component.getName()
                        + "\", $techn=\"Module\", $descr=\"\", $tags=\"\", $link=\"\")");
            }
            indent--;
            line("}");
            line();
            indent--;
            line("}");
            line();
        }
        relationships(view, relationship -> "Rel(" + id(model, relationship.source()) + ", "
                + id(model, relationship.destination()) + ", \"" + relationship.description()
                + "\", $techn=\"\", $tags=\"\", $link=\"\")");
        line("SHOW_LEGEND(true)");
        line("hide stereotypes");
        line("@enduml");
        return text();
    }

    private String uml(DiagramView view, Map<String, String> skinParams) {
        DiagramModel model = view.model();
        List<DiagramComponent> elements = view.elements();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("componentStyle", "uml1");
        params.putAll(skinParams);
        Map<String, String> elementStyles = new TreeMap<>();
        List<String> otherStyles = new ArrayList<>();

        line("@startuml");
        line();
        line("set separator none");
        line("top to bottom direction");
        line("hide stereotype");
        skinParams(params);
        int stylePosition = out.length();
        line();
        if (!elements.isEmpty()) {
            line("rectangle \"" + model.systemName() + "\\n<size:16>[Software System]</size>\" <<"
                    + selector("Boundary", model.systemName()) + ">> {");
            indent++;
            for (DiagramComponent component : elements) {
                String styleName = model.styleName(component);
                String background = model.background(component).orElse(BACKGROUND);
                String stroke = model.background(component).map(Colors::darker).orElse(COLOR);
                elementStyles.put(styleName, elementStyle(styleName, background, stroke));
                line("component \"==" + component.getName() + "\\n<size:16>[Component: Module]</size>\" <<"
                        + selector("Element", styleName) + ">> as " + id(model, component));
            }
            indent--;
            line("}");
            line();
        }
        relationships(view, relationship -> id(model, relationship.source()) + " --> "
                + id(model, relationship.destination()) + " <<" + selector("Relationship", "Relationship") + ">> : \""
                + relationship.description() + "\"");
        if (!view.relationships().isEmpty()) {
            otherStyles.add(style("Relationship", selector("Relationship", "Relationship"), "LineThickness: 2;",
                    "LineStyle: 10-10;", "LineColor: " + COLOR + ";", "FontColor: " + COLOR + ";", "FontSize: 24;"));
        }
        if (!elements.isEmpty()) {
            otherStyles.add(style(model.systemName(), selector("Boundary", model.systemName()),
                    "BackgroundColor: " + BACKGROUND + ";", "LineColor: " + COLOR + ";", "LineStyle: 0;",
                    "LineThickness: 2;", "FontColor: " + COLOR + ";", "FontSize: 24;",
                    "HorizontalAlignment: center;", "Shadowing: 0;"));
        }
        line("@enduml");

        StringBuilder styles = new StringBuilder("<style>\n");
        styles.append("  root {\n    BackgroundColor: ").append(BACKGROUND).append(";\n    FontColor: ").append(COLOR)
                .append(";\n  }\n");
        elementStyles.values().forEach(styles::append);
        otherStyles.forEach(styles::append);
        styles.append("</style>\n");
        out.insert(stylePosition, styles);
        return text();
    }

    private static String elementStyle(String name, String background, String stroke) {
        return style(name, selector("Element", name), "BackgroundColor: " + background + ";",
                "LineColor: " + stroke + ";", "LineStyle: 0;", "LineThickness: 2;", "FontColor: " + COLOR + ";",
                "FontSize: 24;", "HorizontalAlignment: center;", "Shadowing: 0;", "MaximumWidth: 450;");
    }

    private static String style(String name, String selector, String... properties) {
        StringBuilder style = new StringBuilder("  // ").append(name).append("\n  .").append(selector).append(" {\n");
        for (String property : properties) {
            style.append("    ").append(property).append('\n');
        }
        return style.append("  }\n").toString();
    }

    private void skinParams(Map<String, String> skinParams) {
        if (!skinParams.isEmpty()) {
            line();
            line("skinparam {");
            indent++;
            skinParams.forEach((name, value) -> line(name + " " + value));
            indent--;
            line("}");
        }
        line();
    }

    private void relationships(DiagramView view, Function<DiagramRelationship, String> renderer) {
        List<DiagramRelationship> relationships = view.relationships();
        relationships.forEach(relationship -> line(renderer.apply(relationship)));
        if (!relationships.isEmpty()) {
            line();
        }
    }

    private static String selector(String kind, String name) {
        return kind + "-" + Base64.getEncoder().encodeToString(name.getBytes(StandardCharsets.UTF_8));
    }

    private static String id(DiagramModel model, DiagramComponent component) {
        String system = id(model.systemName());
        return system + "." + system + "." + id(component.getName());
    }

    private static String id(String name) {
        return name.replaceAll("(?U)\\W", "");
    }

    private void line() {
        out.append('\n');
    }

    private void line(String content) {
        out.append("  ".repeat(indent)).append(content.replace("\n", "\\n")).append('\n');
    }

    private String text() {
        return out.substring(0, out.length() - 1);
    }
}
