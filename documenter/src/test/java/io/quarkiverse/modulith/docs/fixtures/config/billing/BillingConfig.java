package io.quarkiverse.modulith.docs.fixtures.config.billing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "billing")
public interface BillingConfig extends Defaults {

    @WithDefault("0.1")
    BigDecimal taxRate();

    @WithName("max-lines")
    int maximumLines();

    Optional<String> footer();

    Optional<List<String>> copies();

    List<String> recipients();

    CharSequence label();

    Retry retry();

    Optional<Retry> fallback();

    @WithParentName
    Bank bank();

    String format(String pattern);

    default String describe() {
        return currency();
    }
}
