package io.quarkiverse.modulith.events.deployment.externalization;

import java.util.List;

/**
 * A chain of member accesses starting at the event or at a named bean.
 *
 * @param beanName the bean the chain starts at, empty when it starts at the event
 * @param members the members accessed one after the other
 */
record ExpressionChain(String beanName, List<ExpressionMember> members) {

    ExpressionChain {
        members = List.copyOf(members);
    }

    /**
     * @return whether the chain starts at the event
     */
    boolean startsAtEvent() {
        return beanName.isEmpty();
    }
}
