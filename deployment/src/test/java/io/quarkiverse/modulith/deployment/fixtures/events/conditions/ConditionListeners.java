package io.quarkiverse.modulith.deployment.fixtures.events.conditions;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.quarkiverse.modulith.ApplicationModuleListener;

public class ConditionListeners {

    public static final List<String> HITS = new CopyOnWriteArrayList<>();

    @ApplicationModuleListener(async = false, id = "large", condition = "#event.amount > 100")
    void large(Payment event) {
        HITS.add("large:" + event.reference());
    }

    @ApplicationModuleListener(async = false, id = "euro", condition = "#root.event.currency == 'EUR'")
    void euro(Payment event) {
        HITS.add("euro:" + event.reference());
    }

    @ApplicationModuleListener(async = false, id = "vip", condition = "#a0.customer.vip and not #p0.urgent")
    void vip(Payment event) {
        HITS.add("vip:" + event.reference());
    }

    @ApplicationModuleListener(async = false, id = "open", condition = "event.state eq 'OPEN'")
    void open(Payment event) {
        HITS.add("open:" + event.reference());
    }

    @ApplicationModuleListener(async = false, id = "small", condition = "#payment.amount le 10.5 || #payment.currency == null")
    void small(Payment payment) {
        HITS.add("small:" + payment.reference());
    }

    @ApplicationModuleListener(async = false, id = "ann", condition = "args[0].customer.name == \"Ann\"")
    void ann(Payment event) {
        HITS.add("ann:" + event.reference());
    }

    @ApplicationModuleListener(async = false, id = "urgent", condition = "#root.args[0].urgent")
    void urgent(Payment event) {
        HITS.add("urgent:" + event.reference());
    }

    @ApplicationModuleListener(async = false, id = "range", condition = "!(#event.amount < 5) && #event.amount != 7"
            + " and #event.amount ge 5 and #event.amount lt 1000 and #event.amount ne 500 and #event.amount gt 4")
    void range(Payment event) {
        HITS.add("range:" + event.reference());
    }

    @ApplicationModuleListener(async = false, id = "literals", condition = "(true and false) or null == null")
    void literals(Payment event) {
        HITS.add("literals:" + event.reference());
    }

    @ApplicationModuleListener(async = false, id = "audit", condition = "#event.reference() == 'r-1'")
    void audit(Auditable event) {
        HITS.add("audit:" + event.reference());
    }

    @ApplicationModuleListener(async = false, id = "closed", condition = "#event.state != 'OPEN' and #event.amount <= 100")
    void closed(Payment event) {
        HITS.add("closed:" + event.reference());
    }
}
