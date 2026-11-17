package io.quarkiverse.modulith.core.impl;

import java.util.List;

import io.quarkiverse.modulith.core.ModelData;

/**
 * The outcome of an analysis.
 *
 * @param data the model, not verified yet
 * @param warnings things the user should know, such as deprecated configuration
 */
public record AnalysisResult(ModelData data, List<String> warnings) {
}
