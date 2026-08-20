package org.ether.society.persistence;

import java.time.LocalDateTime;

/**
 * Metadata for a saved simulation.
 */
public class SaveMetadata {
    private String id;
    private String name;
    private LocalDateTime timestamp;
    private long year;
    private int month;
    private String scenarioName;
    private String version;

    public SaveMetadata() {
    }

    public SaveMetadata(String id, String name, long year, int month, String scenarioName) {
        this.id = id;
        this.name = name;
        this.timestamp = LocalDateTime.now();
        this.year = year;
        this.month = month;
        this.scenarioName = scenarioName;
        this.version = "2.0.0";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public long getYear() { return year; }
    public void setYear(long year) { this.year = year; }
    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }
    public String getScenarioName() { return scenarioName; }
    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
}
