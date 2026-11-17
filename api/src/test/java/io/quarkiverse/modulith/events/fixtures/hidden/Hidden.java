package io.quarkiverse.modulith.events.fixtures.hidden;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@interface Hidden {

    String target();
}
