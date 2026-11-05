/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.database.H3Cell;

import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;

/**
 * Virtual {@link NavigableMap} facade backed by on-disk {@code .bin.gz} snapshot archives
 * and bounded LRU heap cache inside {@link HistoryManager}.
 *
 * <p>Implements the complete {@link NavigableMap} contract so timeline scrubbers, replay engines,
 * and counterfactual branchers can query arbitrary simulation ticks without retaining all
 * full-world {@link H3Cell} meshes simultaneously in JVM heap memory.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see HistoryManager
 */
final class VirtualSnapshotNavigableMap extends AbstractMap<Long, List<H3Cell>> implements NavigableMap<Long, List<H3Cell>> {

    /** Owning history manager managing the LRU RAM cache and persistent disk index. */
    private final HistoryManager owner;

    /**
     * Constructs a virtual snapshot map facade bound to the specified {@link HistoryManager}.
     *
     * @param owner owning {@link HistoryManager} instance
     */
    VirtualSnapshotNavigableMap(HistoryManager owner) {
        this.owner = owner;
    }

    private NavigableMap<Long, Path> index() {
        return owner.getDiskSnapshotIndex();
    }

    @Override
    public int size() {
        return index().size();
    }

    @Override
    public boolean isEmpty() {
        return index().isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return index().containsKey(key);
    }

    @Override
    public List<H3Cell> get(Object key) {
        if (!(key instanceof Long tick)) return null;
        return owner.getWorldSnapshot(tick);
    }

    @Override
    public List<H3Cell> put(Long key, List<H3Cell> value) {
        owner.putSnapshotSynchronously(key, value);
        return null;
    }

    @Override
    public List<H3Cell> remove(Object key) {
        if (!(key instanceof Long tick)) return null;
        owner.deleteSnapshot(tick);
        return null;
    }

    @Override
    public void clear() {
        owner.reset();
    }

    @Override
    public Entry<Long, List<H3Cell>> lowerEntry(Long key) {
        Long k = index().lowerKey(key);
        return k != null ? new SimpleImmutableEntry<>(k, owner.getWorldSnapshot(k)) : null;
    }

    @Override
    public Long lowerKey(Long key) {
        return index().lowerKey(key);
    }

    @Override
    public Entry<Long, List<H3Cell>> floorEntry(Long key) {
        Long k = index().floorKey(key);
        return k != null ? new SimpleImmutableEntry<>(k, owner.getWorldSnapshot(k)) : null;
    }

    @Override
    public Long floorKey(Long key) {
        return index().floorKey(key);
    }

    @Override
    public Entry<Long, List<H3Cell>> ceilingEntry(Long key) {
        Long k = index().ceilingKey(key);
        return k != null ? new SimpleImmutableEntry<>(k, owner.getWorldSnapshot(k)) : null;
    }

    @Override
    public Long ceilingKey(Long key) {
        return index().ceilingKey(key);
    }

    @Override
    public Entry<Long, List<H3Cell>> higherEntry(Long key) {
        Long k = index().higherKey(key);
        return k != null ? new SimpleImmutableEntry<>(k, owner.getWorldSnapshot(k)) : null;
    }

    @Override
    public Long higherKey(Long key) {
        return index().higherKey(key);
    }

    @Override
    public Entry<Long, List<H3Cell>> firstEntry() {
        if (index().isEmpty()) return null;
        Long k = index().firstKey();
        return new SimpleImmutableEntry<>(k, owner.getWorldSnapshot(k));
    }

    @Override
    public Entry<Long, List<H3Cell>> lastEntry() {
        if (index().isEmpty()) return null;
        Long k = index().lastKey();
        return new SimpleImmutableEntry<>(k, owner.getWorldSnapshot(k));
    }

    @Override
    public Entry<Long, List<H3Cell>> pollFirstEntry() {
        if (index().isEmpty()) return null;
        Long k = index().firstKey();
        List<H3Cell> val = owner.getWorldSnapshot(k);
        owner.deleteSnapshot(k);
        return new SimpleImmutableEntry<>(k, val);
    }

    @Override
    public Entry<Long, List<H3Cell>> pollLastEntry() {
        if (index().isEmpty()) return null;
        Long k = index().lastKey();
        List<H3Cell> val = owner.getWorldSnapshot(k);
        owner.deleteSnapshot(k);
        return new SimpleImmutableEntry<>(k, val);
    }

    @Override
    public Long firstKey() {
        return index().firstKey();
    }

    @Override
    public Long lastKey() {
        return index().lastKey();
    }

    @Override
    public Set<Long> keySet() {
        return index().keySet();
    }

    @Override
    public NavigableSet<Long> navigableKeySet() {
        return index().navigableKeySet();
    }

    @Override
    public NavigableSet<Long> descendingKeySet() {
        return index().descendingKeySet();
    }

    @Override
    public NavigableMap<Long, List<H3Cell>> descendingMap() {
        return new VirtualSnapshotNavigableMap(owner);
    }

    @Override
    public NavigableMap<Long, List<H3Cell>> subMap(Long fromKey, boolean fromInclusive, Long toKey, boolean toInclusive) {
        return new VirtualSnapshotNavigableMap(owner);
    }

    @Override
    public NavigableMap<Long, List<H3Cell>> headMap(Long toKey, boolean inclusive) {
        return new VirtualSnapshotNavigableMap(owner);
    }

    @Override
    public NavigableMap<Long, List<H3Cell>> tailMap(Long fromKey, boolean inclusive) {
        return new VirtualSnapshotNavigableMap(owner);
    }

    @Override
    public Comparator<? super Long> comparator() {
        return index().comparator();
    }

    @Override
    public SortedMap<Long, List<H3Cell>> subMap(Long fromKey, Long toKey) {
        return subMap(fromKey, true, toKey, false);
    }

    @Override
    public SortedMap<Long, List<H3Cell>> headMap(Long toKey) {
        return headMap(toKey, false);
    }

    @Override
    public SortedMap<Long, List<H3Cell>> tailMap(Long fromKey) {
        return tailMap(fromKey, true);
    }

    @Override
    public Set<Entry<Long, List<H3Cell>>> entrySet() {
        return new AbstractSet<Entry<Long, List<H3Cell>>>() {
            @Override
            public Iterator<Entry<Long, List<H3Cell>>> iterator() {
                Iterator<Long> keyIter = index().keySet().iterator();
                return new Iterator<Entry<Long, List<H3Cell>>>() {
                    @Override
                    public boolean hasNext() {
                        return keyIter.hasNext();
                    }

                    @Override
                    public Entry<Long, List<H3Cell>> next() {
                        Long k = keyIter.next();
                        return new SimpleImmutableEntry<>(k, owner.getWorldSnapshot(k));
                    }
                };
            }

            @Override
            public int size() {
                return index().size();
            }
        };
    }
}
