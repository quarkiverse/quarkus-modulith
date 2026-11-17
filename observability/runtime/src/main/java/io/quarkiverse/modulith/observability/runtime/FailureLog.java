package io.quarkiverse.modulith.observability.runtime;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;

/**
 * Logs failures of the observability itself, which never reach the application: the first failure of each kind as a
 * warning with its stack trace, the repetitions at debug level, so a broken customizer on a hot path does not flood
 * the log.
 */
final class FailureLog {

    private final Logger log;
    private final Set<String> reported = ConcurrentHashMap.newKeySet();

    FailureLog(Class<?> source) {
        this.log = Logger.getLogger(source);
    }

    /**
     * @param kind what failed, for example the class of the failing observer, logged at warning level once
     * @param message what was being done
     */
    void failed(String kind, String message, RuntimeException error) {
        if (reported.add(kind)) {
            log.warnf(error, "%s (%s). Further failures of this kind are logged at debug level", message, kind);
        } else {
            log.debugf(error, "%s (%s)", message, kind);
        }
    }
}
