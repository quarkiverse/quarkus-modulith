package io.quarkiverse.modulith.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Finds the elementary cycles of a module graph whose smallest member is a given module, with the blocking of Johnson's
 * algorithm: a module that cannot lead back to the start on the current path is not entered again until a module it
 * leads to leaves the path. The search costs time linear in the size of the graph per cycle found, and it stops once
 * the given number of cycles is reached. The cycles come in the order of a depth-first search over the sorted
 * dependencies.
 */
final class CycleSearch {

    private final Map<String, Set<String>> graph;
    private final String start;
    private final int limit;
    private final List<List<String>> found;
    private final Deque<String> path = new ArrayDeque<>();
    private final Set<String> blocked = new HashSet<>();
    private final Map<String, Set<String>> blockedBy = new HashMap<>();

    /**
     * @param graph the dependencies of each module, sorted
     * @param start the smallest module of the cycles to find
     * @param limit the number of cycles in {@code found} at which the search stops
     * @param found receives each cycle, its members in path order starting with {@code start}
     */
    CycleSearch(Map<String, Set<String>> graph, String start, int limit, List<List<String>> found) {
        this.graph = graph;
        this.start = start;
        this.limit = limit;
        this.found = found;
    }

    void run() {
        visit(start);
    }

    private boolean visit(String current) {
        boolean closesCycle = false;
        path.addLast(current);
        blocked.add(current);
        for (String next : graph.get(current)) {
            if (found.size() >= limit) {
                return true;
            }
            if (next.equals(start)) {
                found.add(new ArrayList<>(path));
                closesCycle = true;
            } else if (next.compareTo(start) > 0 && !blocked.contains(next) && visit(next)) {
                closesCycle = true;
            }
        }
        if (closesCycle) {
            unblock(current);
        } else {
            graph.get(current).stream().filter(it -> it.compareTo(start) > 0)
                    .forEach(it -> blockedBy.computeIfAbsent(it, key -> new HashSet<>()).add(current));
        }
        path.removeLast();
        return closesCycle;
    }

    private void unblock(String module) {
        blocked.remove(module);
        Optional.ofNullable(blockedBy.remove(module))
                .ifPresent(waiting -> waiting.stream().filter(blocked::contains).forEach(this::unblock));
    }
}
