package org.eclipse.microprofile.reactive.messaging;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Test stand-in for the MicroProfile messaging annotation. */
@Retention(RetentionPolicy.RUNTIME)
public @interface Incoming {
    String value();
}
