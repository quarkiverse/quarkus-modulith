package io.quarkiverse.modulith.deployment.fixtures.events.invalid.order;

import jakarta.transaction.Transactional.TxType;

import io.quarkiverse.modulith.ApplicationModuleListener;

public class UnsupportedAttributesListener {

    @ApplicationModuleListener(readOnlyTransaction = true, propagation = TxType.MANDATORY)
    void mandatory(Character event) {
    }

    @ApplicationModuleListener(condition = "#event.missing > 1")
    void unknownProperty(Amount event) {
    }

    @ApplicationModuleListener(condition = "#event.value >")
    void incomplete(Amount event) {
    }

    @ApplicationModuleListener(condition = "#event.value > 1.2.3")
    void invalidNumber(Amount event) {
    }

    @ApplicationModuleListener(condition = "#event.value().signum() > 1")
    void primitiveHasNoMembers(Amount event) {
    }

    @ApplicationModuleListener(condition = "#event.tags.length > 0")
    void arrayHasNoMembers(Amount event) {
    }

    @ApplicationModuleListener(condition = " ", readOnlyTransaction = false, propagation = TxType.REQUIRES_NEW)
    void defaultsSpelledOut(Byte event) {
    }
}
