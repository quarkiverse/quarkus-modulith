package io.quarkiverse.modulith.core.fixtures.scan;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Makes every kind of reference the scanner records.
 */
@Marker(types = { Target.class, Target[].class }, states = Thread.State.NEW, nested = @Deprecated(since = "1"), many = {
        @Deprecated, @Deprecated(forRemoval = true) }, numbers = { 1, 2 })
public class Scanned<T extends Target> extends java.util.ArrayList<Target.Inner<String>> {

    Target[][] matrix;

    Map<? super Target, ? extends List<?>> wildcards;

    Target.Inner<Target> inner;

    public <X extends Target> X generic(List<X> values) throws IOException {
        return values.get(0);
    }

    Object references(Object value) {
        boolean check = value instanceof Target;
        Class<?> type = Target.class;
        Class<?> arrays = Target[].class;
        Supplier<Target> constructor = Target::new;
        Supplier<Target> factory = Target::create;
        Runnable runnable = () -> {
            Runnable nested = () -> Target.create().run();
            nested.run();
        };
        Object[] copy = new Target[0].clone();
        return check ? type : arrays.isArray() ? constructor.get() : factory.get().equals(copy) ? runnable : copy;
    }

    void annotated(@Marker Target first, int second) {
    }

    class Member {
        Member(@Marker Target target) {
        }
    }
}
