package io.quarkiverse.modulith.deployment.fixtures.events.objects;

import jakarta.enterprise.util.AnnotationLiteral;

public final class AuditedLiteral extends AnnotationLiteral<Audited> implements Audited {

    public static final AuditedLiteral INSTANCE = new AuditedLiteral();

    private AuditedLiteral() {
    }
}
