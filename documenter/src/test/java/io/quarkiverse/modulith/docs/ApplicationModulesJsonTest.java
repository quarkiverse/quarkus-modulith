package io.quarkiverse.modulith.docs;

import static io.quarkiverse.modulith.docs.TestModels.modules;
import static io.quarkiverse.modulith.docs.TestModels.restricted;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.jayway.jsonpath.JsonPath;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.ModelData.ModuleData;
import io.quarkiverse.modulith.docs.metadata.DocumentationSource;

class ApplicationModulesJsonTest {

    @Test
    void writesAllowedDependenciesWithoutSharedModules() {
        // arrange
        ApplicationModules modules = modules("Shop",
                restricted("order", false, List.of("catalog::api", "core", "billing :: *", "catalog::api")),
                restricted("closed", false, List.of()),
                restricted("core", true, List.of()),
                TestModels.module("open", List.of()));
        ModuleDetails order = new ModuleDetails("order", Optional.empty(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), Map.of("api", List.of("com.acme.order.B", "com.acme.order.A")),
                List.of("com.acme.order.Init"));
        DocumentationModel withShared = new DocumentationModel(modules, List.of(order), List.of("core"),
                DocumentationSource.none());
        DocumentationModel withoutShared = new DocumentationModel(modules, List.of(order), List.of(),
                DocumentationSource.none());

        // act
        String full = ApplicationModulesJson.full(withShared);
        String simple = ApplicationModulesJson.simple(withoutShared);

        // assert
        assertThat(full).isEqualTo("{ "
                + "\"closed\" : { \"displayName\" : \"CLOSED\", \"basePackage\" : \"com.acme.closed\", \"nested\" : [], "
                + "\"type\" : \"closed\", \"shared\" : false, \"namedInterfaces\" : {}, \"initializers\" : [], "
                + "\"allowedDependencies\" : [], \"dependencies\" : [] }, "
                + "\"core\" : { \"displayName\" : \"CORE\", \"basePackage\" : \"com.acme.core\", \"nested\" : [], "
                + "\"type\" : \"open\", \"shared\" : true, \"namedInterfaces\" : {}, \"initializers\" : [], "
                + "\"dependencies\" : [] }, "
                + "\"open\" : { \"displayName\" : \"OPEN\", \"basePackage\" : \"com.acme.open\", \"nested\" : [], "
                + "\"type\" : \"closed\", \"shared\" : false, \"namedInterfaces\" : {}, \"initializers\" : [], "
                + "\"dependencies\" : [] }, "
                + "\"order\" : { \"displayName\" : \"ORDER\", \"basePackage\" : \"com.acme.order\", "
                + "\"nested\" : [], \"type\" : \"closed\", \"shared\" : false, \"namedInterfaces\" : { \"api\" : "
                + "[ \"com.acme.order.A\", \"com.acme.order.B\" ] }, \"initializers\" : [ \"com.acme.order.Init\" ], "
                + "\"allowedDependencies\" : [ \"catalog :: api\", \"billing :: *\" ], \"dependencies\" : [] } }");
        assertThat(simple).startsWith("{ \"closed\" : { \"displayName\" : \"CLOSED\", \"basePackage\" : "
                + "\"com.acme.closed\", \"nested\" : [], \"type\" : \"closed\", \"shared\" : false, "
                + "\"dependencies\" : [] }")
                .endsWith("\"order\" : { \"displayName\" : \"ORDER\", \"basePackage\" : \"com.acme.order\", "
                        + "\"nested\" : [], \"type\" : \"closed\", \"shared\" : false, \"allowedDependencies\" : "
                        + "[ \"catalog :: api\", \"core\", \"billing :: *\" ], \"dependencies\" : [] } }");
    }

    @Test
    void escapesQuotesBackslashesAndControlCharacters() {
        // arrange
        ModuleData module = new ModuleData("order", "Say \"hi\" \\ now\n\u0001", "com.acme.order", false, false,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of());
        DocumentationModel model = new DocumentationModel(modules("Shop", module), List.of(), List.of(),
                DocumentationSource.none());

        // act
        String json = ApplicationModulesJson.simple(model);
        String key = ApplicationModulesJson.write(Map.of("a\"b\\c\t", List.of()));

        // assert
        assertThat(json).isEqualTo("{ \"order\" : { \"displayName\" : \"Say \\\"hi\\\" \\\\ now\\u000A\\u0001\", "
                + "\"basePackage\" : \"com.acme.order\", \"nested\" : [], \"type\" : \"closed\", \"shared\" : false, "
                + "\"dependencies\" : [] } }");
        assertThat(JsonPath.<String> read(json, "$.order.displayName")).isEqualTo("Say \"hi\" \\ now\n\u0001");
        assertThat(key).isEqualTo("{ \"a\\\"b\\\\c\\u0009\" : [] }");
        assertThat(JsonPath.<Map<String, Object>> read(key, "$")).containsOnlyKeys("a\"b\\c\t");
    }

    @Test
    void writesAnEmptyModelAsAnEmptyObject() {
        // arrange
        DocumentationModel model = new DocumentationModel(modules("Shop"), List.of(), List.of(),
                DocumentationSource.none());

        // act
        String json = ApplicationModulesJson.simple(model);

        // assert
        assertThat(json).isEqualTo("{}");
    }
}
