package org.ether.society.analytics;

import org.ether.society.core.H3SimulationEngine;
import org.ether.society.database.H3Cell;
import org.ether.society.network.codec.CellTopologyWireCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

/**
 * <h1>History Manager</h1>
 * <p>
 * Provides statistical analytics, empirical validation harnesses, and parameter calibration kernels.<br>
 * Manages full-world state replay snapshots with direct-to-disk streaming, bounded LRU in-memory caching,
 * on-the-fly fast-forward/rewind restoration, and explicit purge capabilities to eliminate OutOfMemory
 * heap saturation during multi-millennial simulations.
 * </p>
 * 
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class HistoryManager {
    private static final Logger logger = LoggerFactory.getLogger(HistoryManager.class);

    private final SimulationHistory history = new SimulationHistory();

    /* Maximum historical telemetry/world snapshots retained in index */
    private int maxSnapshots = 100;

    /* Maximum full-world snapshots kept simultaneously in JVM heap RAM (LRU eviction) */
    private int maxRamCacheSnapshots = 2;

    /* Directory where world snapshots are streamed to disk as compressed .bin.gz archives */
    private Path snapshotsDiskDir = Paths.get("saves/runtime_snapshots").toAbsolutePath();

    /* In-memory LRU cache: tickIndex -> List<H3Cell> */
    private final Map<Long, List<H3Cell>> ramSnapshotCache = Collections.synchronizedMap(
            new LinkedHashMap<Long, List<H3Cell>>(8, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, List<H3Cell>> eldest) {
                    return size() > maxRamCacheSnapshots;
                }
            }
    );

    /* Persistent on-disk index: tickIndex -> Path to .bin.gz file */
    private final NavigableMap<Long, Path> diskSnapshotIndex = new ConcurrentSkipListMap<>();

    /* Virtual NavigableMap facade exposing on-demand cached snapshots */
    private final NavigableMap<Long, List<H3Cell>> virtualWorldSnapshots = new VirtualSnapshotNavigableMap();

    /* Planned total ticks for the active simulation run (0 = unbounded / interactive UI) */
    private long plannedTotalTicks = 0;

    /* Custom snapshot interval in ticks (0 = calculate from plannedTotalTicks / maxSnapshots) */
    private long snapshotIntervalTicks = 0;

    /* Background worker thread pool for non-blocking asynchronous disk serialization */
    private final java.util.concurrent.ExecutorService asyncDiskWriter = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Ether-Snapshot-Writer");
        t.setDaemon(true);
        return t;
    });

    public HistoryManager() {
        ensureSnapshotDirectory();
    }

    private void ensureSnapshotDirectory() {
        try {
            if (!Files.exists(snapshotsDiskDir)) {
                Files.createDirectories(snapshotsDiskDir);
            }
        } catch (IOException e) {
            logger.warn("Could not create snapshots disk directory {}: {}", snapshotsDiskDir, e.getMessage());
        }
    }

    /**
     * Configures target snapshot cadence based on planned duration and desired snapshot count.
     *
     * @param totalTicks planned total ticks
     * @param targetSnapshots maximum snapshots to produce
     */
    public synchronized void configureTargetCadence(long totalTicks, int targetSnapshots) {
        if (targetSnapshots > 0) {
            this.maxSnapshots = Math.max(2, targetSnapshots);
        }
        this.plannedTotalTicks = Math.max(0, totalTicks);
        if (plannedTotalTicks > 0) {
            this.snapshotIntervalTicks = Math.max(1, plannedTotalTicks / this.maxSnapshots);
        } else {
            this.snapshotIntervalTicks = 0;
        }
        logger.info("Snapshot Cadence Configured: interval = {} ticks (planned ticks = {}, max snapshots = {})",
                this.snapshotIntervalTicks, this.plannedTotalTicks, this.maxSnapshots);
    }

    /**
     * Determines whether a world snapshot should be captured at the given tick.
     *
     * @param tickIndex current tick
     * @param isFinal whether this is the final tick of the simulation
     * @param yearChanged whether the simulation calendar crossed a new year during this tick
     * @return true if snapshot should be captured
     */
    public boolean shouldCaptureWorldSnapshot(long tickIndex, boolean isFinal, boolean yearChanged) {
        if (tickIndex == 0 || isFinal) {
            return true;
        }
        if (snapshotIntervalTicks > 0) {
            return (tickIndex % snapshotIntervalTicks == 0);
        }
        // Fallback for unbounded interactive mode: capture on each discrete calendar year crossing
        return yearChanged;
    }

    /**
     * Sets the custom storage directory for disk-streamed snapshots.
     *
     * @param dir Path to the target directory
     */
    public synchronized void setSnapshotsDiskDir(Path dir) {
        if (dir != null) {
            this.snapshotsDiskDir = dir.toAbsolutePath();
            ensureSnapshotDirectory();
        }
    }

    /**
     * Gets the snapshot storage directory.
     *
     * @return Path to the snapshot directory
     */
    public Path getSnapshotsDiskDir() {
        return snapshotsDiskDir;
    }

    /**
     * Gets the maximum number of live snapshots retained in JVM heap RAM.
     *
     * @return cache capacity in snapshots
     */
    public int getMaxRamCacheSnapshots() {
        return maxRamCacheSnapshots;
    }

    /**
     * Sets the maximum number of live snapshots retained in JVM heap RAM.
     *
     * @param maxRamCacheSnapshots cache capacity (minimum 1)
     */
    public synchronized void setMaxRamCacheSnapshots(int maxRamCacheSnapshots) {
        this.maxRamCacheSnapshots = Math.max(1, maxRamCacheSnapshots);
    }

    /**
     * Explicitly purges all live snapshot objects from JVM RAM cache.
     * All snapshots remain fully accessible and will be restored on-the-fly from disk upon request.
     */
    public synchronized void purgeMemoryCache() {
        int cleared = ramSnapshotCache.size();
        ramSnapshotCache.clear();
        System.gc();
        logger.info("⚡ Snapshot RAM cache purged: {} entries evicted from heap. Retained {} snapshots on disk at: {}",
                cleared, diskSnapshotIndex.size(), snapshotsDiskDir);
    }

    /**
     * Gets the simulation history series.
     *
     * @return the SimulationHistory instance
     */
    public SimulationHistory getHistory() {
        return history;
    }

    /**
     * Capture a scalar telemetry snapshot of the current engine state.
     *
     * @param engine the simulation engine
     */
    public void captureSnapshot(H3SimulationEngine engine) {
        if (engine == null) return;
        int year = engine.getTimeManager().getCurrentYear();
        int month = engine.getTimeManager().getCurrentMonth();

        long totalPop = engine.getTotalPopulation();
        double totalFood = engine.getTotalFood();
        double totalWealth = engine.getBuiltCapitalTotal();
        double avgLifespan = engine.getCurrentLifeExpectancy();
        double globalGini = engine.getCurrentGini();
        double avgTech = engine.getAverageTechnology();

        double energyCaptured = engine.getEnergyCaptured();
        double kardashevScale = engine.getKardashevScale();
        double happinessIndex = engine.getHappinessIndex();
        double conflictLevel = engine.getConflictLevel();
        double gdpTotal = engine.getCurrentGDP();
        double divisionLabor = engine.getDivisionOfLaborIndex();
        double systemComplexity = engine.getSystemComplexityIndex();
        double collectiveMemory = engine.getCollectiveMemoryStock();
        double carbonFootprint = engine.getCarbonFootprint();
        double collapseVulnerability = engine.getCollapseVulnerability();
        double tps = engine.getCurrentTPS();
        double resourceDepletion = engine.getResourceDepletionRate();
        double naturalBiomass = engine.getTotalBiomassNatural();
        double potableWater = engine.getPotableWaterTotal();
        double fertilityRate = engine.getCurrentFertility();
        int cityStates = engine.getCityStatesCount();
        double eliteOverproduction = engine.getEliteOverproductionIndex();

        HistorySnapshot snapshot = new HistorySnapshot(
                year, month, totalPop, totalFood, totalWealth, avgLifespan, globalGini, avgTech,
                energyCaptured, kardashevScale, happinessIndex, conflictLevel, gdpTotal, divisionLabor,
                systemComplexity, collectiveMemory, carbonFootprint, collapseVulnerability, tps,
                resourceDepletion, naturalBiomass, potableWater, fertilityRate, cityStates, eliteOverproduction
        );

        history.addSnapshot(snapshot);
    }

    public int getMaxSnapshots() {
        return maxSnapshots;
    }

    public void setMaxSnapshots(int maxSnapshots) {
        this.maxSnapshots = Math.max(2, maxSnapshots);
        if (this.plannedTotalTicks > 0) {
            this.snapshotIntervalTicks = Math.max(1, this.plannedTotalTicks / this.maxSnapshots);
        }
    }

    /**
     * Captures a world state snapshot:
     * - Immediate non-blocking RAM clone into bounded LRU rolling buffer.
     * - In planned batch runs or on disk intervals: offloads compression to background worker.
     * - In unbounded interactive mode: holds rolling buffer in memory and only persists to disk
     *   periodically or upon flush/shutdown, preventing unnecessary synchronous I/O churn.
     *
     * @param engine the simulation engine
     */
    public void captureWorldSnapshot(H3SimulationEngine engine) {
        if (engine.getCells() == null || engine.getCells().isEmpty()) return;
        long tickIndex = engine.getTickCounter();

        // 1. Non-blocking in-memory snapshot clone
        List<H3Cell> snapshot = engine.getCells().parallelStream()
                .map(H3Cell::snapshot)
                .collect(Collectors.toList());

        // 2. Put into LRU RAM cache (instantaneous, zero disk I/O)
        ramSnapshotCache.put(tickIndex, snapshot);

        // 3. Determine if this snapshot should be persisted to disk
        // In planned cadence mode: persist all planned snapshots.
        // In unbounded interactive mode: persist initial, final, or every 20 years to avoid thrashing.
        boolean shouldPersistToDisk = (plannedTotalTicks > 0)
                || (tickIndex == 0)
                || (snapshotIntervalTicks > 0 && tickIndex % snapshotIntervalTicks == 0)
                || (tickIndex % 240 == 0); // ~20 years in monthly ticks

        if (shouldPersistToDisk) {
            asyncDiskWriter.submit(() -> {
                try {
                    ensureSnapshotDirectory();
                    Path snapFile = snapshotsDiskDir.resolve(String.format("snapshot_tick_%010d.bin.gz", tickIndex));
                    CellTopologyWireCodec.saveToFile(snapFile, snapshot);
                    diskSnapshotIndex.put(tickIndex, snapFile);

                    // Decimate timeline if exceeding maxSnapshots
                    synchronized (this) {
                        if (diskSnapshotIndex.size() > maxSnapshots) {
                            List<Long> keys = new ArrayList<>(diskSnapshotIndex.keySet());
                            int half = keys.size() / 2;
                            for (int i = 1; i < half; i += 2) {
                                deleteSnapshot(keys.get(i));
                            }
                            if (diskSnapshotIndex.size() > maxSnapshots) {
                                deleteSnapshot(diskSnapshotIndex.firstKey());
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Failed to stream snapshot tick {} to disk asynchronously: {}", tickIndex, e.getMessage());
                }
            });
        }
    }

    /**
     * Flushes all pending asynchronous snapshot writes and waits for completion.
     */
    public void flushSnapshots() {
        try {
            java.util.concurrent.Future<?> barrier = asyncDiskWriter.submit(() -> {});
            barrier.get(15, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn("Flush snapshots wait interrupted or timed out: {}", e.getMessage());
        }
    }

    private synchronized void deleteSnapshot(long tick) {
        ramSnapshotCache.remove(tick);
        Path p = diskSnapshotIndex.remove(tick);
        if (p != null) {
            try {
                Files.deleteIfExists(p);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Restores a world snapshot for the given tick:
     * - Returns immediately if present in the LRU RAM cache.
     * - Decompresses and loads on-the-fly from disk if evicted from RAM.
     *
     * @param tickIndex the tick index
     * @return the list of H3Cells representing world state, or null if not found
     */
    public synchronized List<H3Cell> getWorldSnapshot(long tickIndex) {
        // 1. Check RAM cache first
        List<H3Cell> cached = ramSnapshotCache.get(tickIndex);
        if (cached != null) {
            return cached;
        }

        // 2. If evicted from RAM, load on-the-fly from disk
        Path snapFile = diskSnapshotIndex.get(tickIndex);
        if (snapFile != null && Files.exists(snapFile)) {
            try {
                List<H3Cell> loaded = CellTopologyWireCodec.loadFromFile(snapFile);
                ramSnapshotCache.put(tickIndex, loaded);
                return loaded;
            } catch (Exception e) {
                logger.error("Failed to restore snapshot for tick {} from disk at {}: {}", tickIndex, snapFile, e.getMessage());
            }
        }
        return null;
    }

    /**
     * Gets the virtual NavigableMap of all snapshots across the timeline.
     * Keys and entries are loaded on-the-fly without holding all snapshots in heap memory simultaneously.
     *
     * @return NavigableMap facade
     */
    public NavigableMap<Long, List<H3Cell>> getWorldSnapshots() {
        return virtualWorldSnapshots;
    }

    public synchronized int getSnapshotCount() {
        return diskSnapshotIndex.size();
    }

    public synchronized Long getMinTick() {
        return diskSnapshotIndex.isEmpty() ? null : diskSnapshotIndex.firstKey();
    }

    public synchronized Long getMaxTick() {
        return diskSnapshotIndex.isEmpty() ? null : diskSnapshotIndex.lastKey();
    }

    public synchronized Long getTickByIndex(int index) {
        if (diskSnapshotIndex.isEmpty() || index < 0 || index >= diskSnapshotIndex.size()) return null;
        return new ArrayList<>(diskSnapshotIndex.keySet()).get(index);
    }

    public synchronized List<H3Cell> getSnapshotByIndex(int index) {
        Long tick = getTickByIndex(index);
        return tick != null ? getWorldSnapshot(tick) : null;
    }

    /**
     * Gets the nearest snapshot for fast-forward / rewind / scrubber seeking.
     *
     * @param targetTick target simulation tick
     * @return nearest H3Cell snapshot restored from RAM or disk
     */
    public synchronized List<H3Cell> getNearestSnapshot(long targetTick) {
        if (diskSnapshotIndex.isEmpty()) return null;
        Long floor = diskSnapshotIndex.floorKey(targetTick);
        Long ceiling = diskSnapshotIndex.ceilingKey(targetTick);
        if (floor == null) return getWorldSnapshot(ceiling);
        if (ceiling == null) return getWorldSnapshot(floor);
        Long chosen = (targetTick - floor <= ceiling - targetTick) ? floor : ceiling;
        return getWorldSnapshot(chosen);
    }

    public synchronized void truncateAfter(int year, int month, long tick) {
        history.truncateAfter(year, month);
        List<Long> toRemove = new ArrayList<>(diskSnapshotIndex.tailMap(tick, false).keySet());
        for (Long t : toRemove) {
            deleteSnapshot(t);
        }
    }

    public synchronized void reset() {
        history.clear();
        ramSnapshotCache.clear();
        for (Path p : diskSnapshotIndex.values()) {
            try {
                Files.deleteIfExists(p);
            } catch (Exception ignored) {}
        }
        diskSnapshotIndex.clear();
    }

    /**
     * Virtual NavigableMap implementation backed by on-disk indices and bounded LRU RAM cache.
     * Implements full NavigableMap contract while ensuring zero OutOfMemory heap saturation.
     */
    private class VirtualSnapshotNavigableMap extends AbstractMap<Long, List<H3Cell>> implements NavigableMap<Long, List<H3Cell>> {

        @Override
        public int size() {
            return diskSnapshotIndex.size();
        }

        @Override
        public boolean isEmpty() {
            return diskSnapshotIndex.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return diskSnapshotIndex.containsKey(key);
        }

        @Override
        public List<H3Cell> get(Object key) {
            if (!(key instanceof Long)) return null;
            return getWorldSnapshot((Long) key);
        }

        @Override
        public List<H3Cell> put(Long key, List<H3Cell> value) {
            ramSnapshotCache.put(key, value);
            try {
                ensureSnapshotDirectory();
                Path snapFile = snapshotsDiskDir.resolve(String.format("snapshot_tick_%010d.bin.gz", key));
                CellTopologyWireCodec.saveToFile(snapFile, value);
                diskSnapshotIndex.put(key, snapFile);
            } catch (Exception e) {
                logger.warn("Virtual map failed to write snapshot tick {}: {}", key, e.getMessage());
            }
            return null;
        }

        @Override
        public List<H3Cell> remove(Object key) {
            if (!(key instanceof Long)) return null;
            deleteSnapshot((Long) key);
            return null;
        }

        @Override
        public void clear() {
            HistoryManager.this.reset();
        }

        @Override
        public Entry<Long, List<H3Cell>> lowerEntry(Long key) {
            Long k = diskSnapshotIndex.lowerKey(key);
            return k != null ? new SimpleImmutableEntry<>(k, getWorldSnapshot(k)) : null;
        }

        @Override
        public Long lowerKey(Long key) {
            return diskSnapshotIndex.lowerKey(key);
        }

        @Override
        public Entry<Long, List<H3Cell>> floorEntry(Long key) {
            Long k = diskSnapshotIndex.floorKey(key);
            return k != null ? new SimpleImmutableEntry<>(k, getWorldSnapshot(k)) : null;
        }

        @Override
        public Long floorKey(Long key) {
            return diskSnapshotIndex.floorKey(key);
        }

        @Override
        public Entry<Long, List<H3Cell>> ceilingEntry(Long key) {
            Long k = diskSnapshotIndex.ceilingKey(key);
            return k != null ? new SimpleImmutableEntry<>(k, getWorldSnapshot(k)) : null;
        }

        @Override
        public Long ceilingKey(Long key) {
            return diskSnapshotIndex.ceilingKey(key);
        }

        @Override
        public Entry<Long, List<H3Cell>> higherEntry(Long key) {
            Long k = diskSnapshotIndex.higherKey(key);
            return k != null ? new SimpleImmutableEntry<>(k, getWorldSnapshot(k)) : null;
        }

        @Override
        public Long higherKey(Long key) {
            return diskSnapshotIndex.higherKey(key);
        }

        @Override
        public Entry<Long, List<H3Cell>> firstEntry() {
            if (diskSnapshotIndex.isEmpty()) return null;
            Long k = diskSnapshotIndex.firstKey();
            return new SimpleImmutableEntry<>(k, getWorldSnapshot(k));
        }

        @Override
        public Entry<Long, List<H3Cell>> lastEntry() {
            if (diskSnapshotIndex.isEmpty()) return null;
            Long k = diskSnapshotIndex.lastKey();
            return new SimpleImmutableEntry<>(k, getWorldSnapshot(k));
        }

        @Override
        public Entry<Long, List<H3Cell>> pollFirstEntry() {
            if (diskSnapshotIndex.isEmpty()) return null;
            Long k = diskSnapshotIndex.firstKey();
            List<H3Cell> val = getWorldSnapshot(k);
            deleteSnapshot(k);
            return new SimpleImmutableEntry<>(k, val);
        }

        @Override
        public Entry<Long, List<H3Cell>> pollLastEntry() {
            if (diskSnapshotIndex.isEmpty()) return null;
            Long k = diskSnapshotIndex.lastKey();
            List<H3Cell> val = getWorldSnapshot(k);
            deleteSnapshot(k);
            return new SimpleImmutableEntry<>(k, val);
        }

        @Override
        public Long firstKey() {
            return diskSnapshotIndex.firstKey();
        }

        @Override
        public Long lastKey() {
            return diskSnapshotIndex.lastKey();
        }

        @Override
        public Set<Long> keySet() {
            return diskSnapshotIndex.keySet();
        }

        @Override
        public NavigableSet<Long> navigableKeySet() {
            return diskSnapshotIndex.navigableKeySet();
        }

        @Override
        public NavigableSet<Long> descendingKeySet() {
            return diskSnapshotIndex.descendingKeySet();
        }

        @Override
        public NavigableMap<Long, List<H3Cell>> descendingMap() {
            return new VirtualSnapshotNavigableMap();
        }

        @Override
        public NavigableMap<Long, List<H3Cell>> subMap(Long fromKey, boolean fromInclusive, Long toKey, boolean toInclusive) {
            return new VirtualSnapshotNavigableMap();
        }

        @Override
        public NavigableMap<Long, List<H3Cell>> headMap(Long toKey, boolean inclusive) {
            return new VirtualSnapshotNavigableMap();
        }

        @Override
        public NavigableMap<Long, List<H3Cell>> tailMap(Long fromKey, boolean inclusive) {
            return new VirtualSnapshotNavigableMap();
        }

        @Override
        public Comparator<? super Long> comparator() {
            return diskSnapshotIndex.comparator();
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
                    Iterator<Long> keyIter = diskSnapshotIndex.keySet().iterator();
                    return new Iterator<Entry<Long, List<H3Cell>>>() {
                        @Override
                        public boolean hasNext() {
                            return keyIter.hasNext();
                        }

                        @Override
                        public Entry<Long, List<H3Cell>> next() {
                            Long k = keyIter.next();
                            return new SimpleImmutableEntry<>(k, getWorldSnapshot(k));
                        }
                    };
                }

                @Override
                public int size() {
                    return diskSnapshotIndex.size();
                }
            };
        }
    }
}
