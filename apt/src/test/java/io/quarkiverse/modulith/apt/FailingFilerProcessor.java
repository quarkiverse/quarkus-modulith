package io.quarkiverse.modulith.apt;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

/**
 * Runs a processor with a filer that cannot create resources.
 */
final class FailingFilerProcessor extends AbstractProcessor {

    private final AbstractProcessor delegate;

    FailingFilerProcessor(AbstractProcessor delegate) {
        this.delegate = delegate;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return delegate.getSupportedAnnotationTypes();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return delegate.getSupportedSourceVersion();
    }

    @Override
    public synchronized void init(ProcessingEnvironment environment) {
        super.init(environment);
        delegate.init(new FailingEnvironment(environment));
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        return delegate.process(annotations, roundEnv);
    }
}
