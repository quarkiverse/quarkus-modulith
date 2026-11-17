/**
 * Shared kernel. Open, so every module may use all of its types, and started in every module test.
 */
@ApplicationModule(type = ApplicationModule.Type.OPEN)
package io.quarkiverse.modulith.it.common;

import io.quarkiverse.modulith.ApplicationModule;
