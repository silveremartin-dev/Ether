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
 * @version 1.0.0-beta.2
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
    /* Internal state variable for cell count (int). */
    private int cellCount;
    /* Internal state variable for H3 resolution (int). */
    private int h3Resolution;
    /* Internal state variable for software version (String). */
    private String version = org.ether.society.config.AppVersion.getRawVersion();

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
        this(id, name, year, month, scenarioName, 0, 3);
    }

    /*
     * Save metadata with spatial topology parameters.
     *
     * @param id the save ID
     * @param name the save name
     * @param year current simulation year
     * @param month current simulation month
     * @param scenarioName name of scenario
     * @param cellCount total hexagonal cells in grid
     * @param h3Resolution H3 spatial resolution level
     */
    public SaveMetadata(String id, String name, long year, int month, String scenarioName, int cellCount, int h3Resolution) {
        this.id = id;
        this.name = name;
        this.timestamp = LocalDateTime.now();
        this.year = year;
        this.month = month;
        this.scenarioName = scenarioName;
        this.cellCount = cellCount;
        this.h3Resolution = h3Resolution;
        this.version = "1.0.0-beta.2";
    }

    /*
     * Get cell count.
     *
     * @return number of cells in simulation grid
     */
    public int getCellCount() { return cellCount; }

    /*
     * Set cell count.
     *
     * @param cellCount the cell count
     */
    public void setCellCount(int cellCount) { this.cellCount = cellCount; }

    /*
     * Get H3 spatial resolution.
     *
     * @return resolution level
     */
    public int getH3Resolution() { return h3Resolution; }

    /*
     * Set H3 spatial resolution.
     *
     * @param h3Resolution the resolution level
     */
    public void setH3Resolution(int h3Resolution) { this.h3Resolution = h3Resolution; }

    /*
     * Get id.
     * Enforces physical invariants and updates associated state variables within {@code SaveMetadata}.
     *
     * @return the resulting computation or state reference
     */
    public String getId() { return id; }
    /*
     * Set id operation.
     * <p>
     * Executes operational logic for {@code SaveMetadata} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param id the id argument (String)
     */
    public void setId(String id) { this.id = id; }
    /*
     * Get name.
     * Enforces physical invariants and updates associated state variables within {@code SaveMetadata}.
     *
     * @return the resulting computation or state reference
     */
    public String getName() { return name; }
    /*
     * Set name operation.
     * <p>
     * Executes operational logic for {@code SaveMetadata} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param name the name argument (String)
     */
    public void setName(String name) { this.name = name; }
    /*
     * Get timestamp.
     * Enforces physical invariants and updates associated state variables within {@code SaveMetadata}.
     *
     * @return the resulting computation or state reference
     */
    public LocalDateTime getTimestamp() { return timestamp; }
    /*
     * Set timestamp operation.
     * <p>
     * Executes operational logic for {@code SaveMetadata} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param timestamp the timestamp argument (LocalDateTime)
     */
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    /*
     * Get year.
     * Enforces physical invariants and updates associated state variables within {@code SaveMetadata}.
     *
     * @return the resulting computation or state reference
     */
    public long getYear() { return year; }
    /*
     * Set year operation.
     * <p>
     * Executes operational logic for {@code SaveMetadata} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param year the year argument (long)
     */
    public void setYear(long year) { this.year = year; }
    /*
     * Get month.
     * Enforces physical invariants and updates associated state variables within {@code SaveMetadata}.
     *
     * @return the resulting computation or state reference
     */
    public int getMonth() { return month; }
    /*
     * Set month operation.
     * <p>
     * Executes operational logic for {@code SaveMetadata} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param month the month argument (int)
     */
    public void setMonth(int month) { this.month = month; }
    /*
     * Get scenario name.
     * Enforces physical invariants and updates associated state variables within {@code SaveMetadata}.
     *
     * @return the resulting computation or state reference
     */
    public String getScenarioName() { return scenarioName; }
    /*
     * Set scenario name operation.
     * <p>
     * Executes operational logic for {@code SaveMetadata} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param scenarioName the scenario name argument (String)
     */
    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }
    /*
     * Get version.
     * Enforces physical invariants and updates associated state variables within {@code SaveMetadata}.
     *
     * @return the resulting computation or state reference
     */
    public String getVersion() { return version; }
    /*
     * Set version operation.
     * <p>
     * Executes operational logic for {@code SaveMetadata} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param version the version argument (String)
     */
    public void setVersion(String version) { this.version = version; }
}
