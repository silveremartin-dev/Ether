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

    /*
     * Add snapshot.
     * Enforces physical invariants and updates associated state variables within {@code SimulationHistory}.
     *
     * @param snapshot the snapshot parameter (HistorySnapshot)
     */
    public void addSnapshot(HistorySnapshot snapshot) {
        synchronized (snapshots) {
            if (snapshots.size() >= 10000) {
                snapshots.remove(0);
            }
            snapshots.add(snapshot);
        }
    }

    /*
     * Get snapshots.
     * Enforces physical invariants and updates associated state variables within {@code SimulationHistory}.
     *
     * @return the resulting computation or state reference
     */
    public List<HistorySnapshot> getSnapshots() {
        synchronized (snapshots) {
            return Collections.unmodifiableList(new ArrayList<>(snapshots));
        }
    }

    /*
     * Clear.
     * Enforces physical invariants and updates associated state variables within {@code SimulationHistory}.
     *
     */
    public void clear() {
        synchronized (snapshots) {
            snapshots.clear();
        }
    }

    /*
     * Truncate after.
     * Enforces physical invariants and updates associated state variables within {@code SimulationHistory}.
     *
     * @param year the year parameter (int)
     * @param month the month parameter (int)
     */
    public void truncateAfter(int year, int month) {
        synchronized (snapshots) {
            snapshots.removeIf(s -> s.year() > year || (s.year() == year && s.month() > month));
        }
    }
}
