package io.quarkiverse.modulith.core.par.interfaces.viewer;

import io.quarkiverse.modulith.core.par.interfaces.report.internal.Defaulted;
import io.quarkiverse.modulith.core.par.interfaces.report.internal.Exporter;
import io.quarkiverse.modulith.core.par.interfaces.report.internal.Hidden;
import io.quarkiverse.modulith.core.par.interfaces.report.model.Row;
import io.quarkiverse.modulith.core.par.interfaces.report.model.detail.Cell;

public class Viewer {
    Row row;
    Cell cell;
    Exporter exporter;
    Defaulted defaulted;
    Hidden hidden;
}
