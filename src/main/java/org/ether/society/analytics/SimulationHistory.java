package org.ether.society.analytics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Container for the entire simulation history.
 * Manages the list of snapshots.
 */
public class SimulationHistory {
    private final List<HistorySnapshot> snapshots = new ArrayList<>();

    public void addSnapshot(HistorySnapshot snapshot) {
        synchronized (snapshots) {
            if (snapshots.size() >= 10000) {
                snapshots.remove(0);
            }
            snapshots.add(snapshot);
        }
    }

    public List<HistorySnapshot> getSnapshots() {
        synchronized (snapshots) {
            return Collections.unmodifiableList(new ArrayList<>(snapshots));
        }
    }

    public void clear() {
        synchronized (snapshots) {
            snapshots.clear();
        }
    }
}
