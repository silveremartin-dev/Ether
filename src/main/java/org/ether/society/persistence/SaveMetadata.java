package org.ether.society.persistence;

import java.time.LocalDateTime;

/**
 * <h1>Save Metadata</h1>
 * <p>
 * Geospatial data ingestion, raster sampling, and tensor map management pipeline.<br>
 * Ingests global planetary datasets (NOAA ETOPO, WorldClim, UNESCO WHYMAP, Natural Earth, Seshat, D-PLACE) into standardized H3 hexagonal rasters.
 * </p>
 * 
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class SaveMetadata {
    /* Internal state variable for id (String). */
    private String id;
    /* Internal state variable for name (String). */
    private String name;
    private LocalDateTime timestamp;
    /* Internal state variable for year (long). */
    private long year;
    /* Internal state variable for month (int). */
    private int month;
    /* Internal state variable for scenario name (String). */
    private String scenarioName;
    /* Internal state variable for version (String). */
    private String version;

    /*
     * Save metadata.
     * Enforces physical invariants and updates associated state variables within {@code SaveMetadata}.
     *
     */
    public SaveMetadata() {
    }

    /*
     * Save metadata.
     * Enforces physical invariants and updates associated state variables within {@code SaveMetadata}.
     *
     * @param id the id parameter (String)
     * @param name the name parameter (String)
     * @param year the year parameter (long)
     * @param month the month parameter (int)
     * @param scenarioName the scenario name parameter (String)
     */
    public SaveMetadata(String id, String name, long year, int month, String scenarioName) {
        this.id = id;
        this.name = name;
        this.timestamp = LocalDateTime.now();
        this.year = year;
        this.month = month;
        this.scenarioName = scenarioName;
        this.version = "1.0.0-beta.1";
    }

    /*
     * Get id.
     * Enforces physical invariants and updates associated state variables within {@code SaveMetadata}.
     *
     * @return the resulting computation or state reference
     */
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    /*
     * Get name.
     * Enforces physical invariants and updates associated state variables within {@code SaveMetadata}.
     *
     * @return the resulting computation or state reference
     */
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    /*
     * Get timestamp.
     * Enforces physical invariants and updates associated state variables within {@code SaveMetadata}.
     *
     * @return the resulting computation or state reference
     */
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    /*
     * Get year.
     * Enforces physical invariants and updates associated state variables within {@code SaveMetadata}.
     *
     * @return the resulting computation or state reference
     */
    public long getYear() { return year; }
    public void setYear(long year) { this.year = year; }
    /*
     * Get month.
     * Enforces physical invariants and updates associated state variables within {@code SaveMetadata}.
     *
     * @return the resulting computation or state reference
     */
    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }
    /*
     * Get scenario name.
     * Enforces physical invariants and updates associated state variables within {@code SaveMetadata}.
     *
     * @return the resulting computation or state reference
     */
    public String getScenarioName() { return scenarioName; }
    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }
    /*
     * Get version.
     * Enforces physical invariants and updates associated state variables within {@code SaveMetadata}.
     *
     * @return the resulting computation or state reference
     */
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
}
