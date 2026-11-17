package io.quarkiverse.modulith.core.par.shapes.caught;

import java.util.concurrent.Callable;

import io.quarkiverse.modulith.core.par.shapes.target.internal.Problem;

public class Catcher {

    public Object call(Callable<Object> callable) {
        try {
            return callable.call();
        } catch (Problem e) {
            return "problem";
        } catch (Exception e) {
            return "other";
        }
    }
}
