/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Audit Log & Interactive Timeline for a Scenario.
 * Records the initial scenario setup (T_0) and all live "God Mode" interventions
 * or planetary climate events triggered during execution.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class ScenarioTimeline implements Serializable {
    private static final long serialVersionUID = 1L;

    public record TimelineEntry(
        long year,
        String eventType,
        String title,
        String details,
        boolean isGodModeIntervention,
        double latitude,
        double longitude,
        double magnitude,
        int durationDays
    ) implements Serializable {
        public TimelineEntry(long year, String eventType, String title, String details, boolean isGodModeIntervention) {
            this(year, eventType, title, details, isGodModeIntervention, 0.0, 0.0, 1.0, 30);
        }
    }

    private final List<TimelineEntry> entries = new ArrayList<>();

    public ScenarioTimeline() {}

    public void addEntry(long year, String eventType, String title, String details, boolean isGodModeIntervention) {
        addEntry(year, eventType, title, details, isGodModeIntervention, 0.0, 0.0, 1.0, 30);
    }

    public void addEntry(long year, String eventType, String title, String details, boolean isGodModeIntervention,
                         double latitude, double longitude, double magnitude, int durationDays) {
        entries.add(new TimelineEntry(year, eventType, title, details, isGodModeIntervention, latitude, longitude, magnitude, durationDays));
        Collections.sort(entries, (a, b) -> Long.compare(a.year(), b.year()));
    }

    public List<TimelineEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public void clear() {
        entries.clear();
    }

    public void truncateAfter(long year) {
        entries.removeIf(e -> e.year() > year);
    }
}
