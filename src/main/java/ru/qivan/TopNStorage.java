package ru.qivan;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TopNStorage<T extends Comparable<T>> {

    private final int n;
    private final NavigableMap<T, Integer> store = new TreeMap<>();

    public TopNStorage(int n) {
        this.n = n;
    }

    public synchronized void add(T element) {
        store.compute(element, (elem, count) -> {
            if (count == null) {
                return 1;
            } else {
                return count + 1;
            }
        });
    }

    public synchronized List<T> getTop() {
        return store.entrySet().stream()
                .sorted(Comparator.<Map.Entry<T, Integer>>comparingInt(Map.Entry::getValue).reversed())
                .limit(n)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public synchronized Map<T, Integer> getStore() {
        return store;
    }

}
