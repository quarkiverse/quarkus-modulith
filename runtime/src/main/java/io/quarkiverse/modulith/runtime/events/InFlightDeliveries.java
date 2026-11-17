package io.quarkiverse.modulith.runtime.events;

import java.time.Duration;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Counts the deliveries that are queued or running, and lets callers wait until there are none. Uses a lock and a
 * condition instead of polling, and no {@code synchronized} block, so it is safe on virtual threads.
 */
final class InFlightDeliveries {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition idle = lock.newCondition();
    private int count;
    private boolean stopped;

    /**
     * Counts a delivery in flight, also after {@link #stop()}.
     */
    void begin() {
        lock.lock();
        try {
            count++;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Counts a delivery in flight unless {@link #stop()} was called. Testing and counting under one lock keeps a
     * delivery from starting after the shutdown found nothing in flight.
     *
     * @return {@code false} if the deliveries are stopped and this one must not start
     */
    boolean tryBegin() {
        lock.lock();
        try {
            if (stopped) {
                return false;
            }
            count++;
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Refuses the deliveries {@link #tryBegin()} would start from now on.
     */
    void stop() {
        lock.lock();
        try {
            stopped = true;
        } finally {
            lock.unlock();
        }
    }

    void end() {
        lock.lock();
        try {
            count--;
            if (count == 0) {
                idle.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    int count() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return {@code true} if no delivery was in flight before the timeout elapsed, {@code false} on timeout or when
     *         the thread was interrupted
     */
    boolean awaitIdle(Duration timeout) {
        long remaining = timeout.toNanos();
        lock.lock();
        try {
            while (count > 0) {
                if (remaining <= 0) {
                    return false;
                }
                remaining = idle.awaitNanos(remaining);
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            lock.unlock();
        }
    }
}
