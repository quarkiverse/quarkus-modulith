package io.quarkiverse.modulith.events.deployment.externalization.fixtures.compile;

import java.time.Duration;

import io.quarkiverse.modulith.events.deployment.externalization.fixtures.compile.other.BaseWithHidden;

public class CompileEvent extends BaseWithHidden implements Labeled {

    public static String STATIC_FIELD = "static";

    public int count;

    private String secret;

    public String getName() {
        return "name";
    }

    public boolean isActive() {
        return true;
    }

    public Boolean isBoxed() {
        return Boolean.TRUE;
    }

    public int isCounted() {
        return 1;
    }

    LocalThing thing() {
        return new LocalThing();
    }

    public String code() {
        return "code";
    }

    public static String staticOne() {
        return "static";
    }

    public int[] tags() {
        return new int[0];
    }

    public Duration duration() {
        return Duration.ZERO;
    }

    public Labeled labeled() {
        return this;
    }

    public GenericHolder<String> holder() {
        return new GenericHolder<>();
    }

    private String privy() {
        return secret;
    }

    String local() {
        return "local";
    }

    @Override
    public String label() {
        return "label";
    }
}
