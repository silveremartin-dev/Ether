/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Author: Silvere Martin-Michiellot (silvere.martin@gmail.com)
 * Contributors: AI Assistant (Antigravity/Claude)
 */
package org.ether.society.database;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a simulation instance with its state and parameters.
 * Multiple simulations can run on the same WorldMap with different settings.
 *
 * <p>
 * Each simulation tracks:
 * </p>
 * <ul>
 * <li>Time progression (start date, current date, time step)</li>
 * <li>Linked world map and H3 cells</li>
 * <li>Density maps (population, resources)</li>
 * <li>Simulation parameters</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1-beta.1
 * @since 1.0.0
 */
@Entity
@Table(name = "simulations")
public class Simulation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Simulation name (e.g., "Neolithic Europe", "Ice Age Simulation").
     */
    @Column(nullable = false)
    private String name;

    /**
     * Description of simulation scenario.
     */
    @Column(length = 2000)
    private String description;

    /**
     * Reference to the WorldMap being used.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "world_map_id", nullable = false)
    private WorldMap worldMap;

    // Time configuration

    /**
     * Simulation start year (e.g., -20000 for 20,000 BC).
     */
    @Column(nullable = false)
    private Integer startYear;

    /**
     * Current simulated year.
     */
    @Column(nullable = false)
    private Integer currentYear;

    /**
     * Current simulated month (0-11).
     */
    @Column(nullable = false)
    private Integer currentMonth = 0;

    /**
     * Time step size: MONTH, SEASON, YEAR, DECADE.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimeStep timeStep = TimeStep.MONTH;

    /**
     * Total ticks (updates) executed.
     */
    @Column(nullable = false)
    private Long totalTicks = 0L;

    // Simulation state

    /**
     * Current state: CREATED, RUNNING, PAUSED, COMPLETED.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SimulationState state = SimulationState.CREATED;

    /**
     * Total human population across all cells.
     */
    @Column
    private Long totalPopulation = 0L;

    /**
     * Global temperature offset (for climate events).
     */
    @Column
    private Double globalTemperatureOffset = 0.0;

    // Metadata

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column
    private LocalDateTime lastTickAt;

    // Enums

    public enum TimeStep {
        /** One month per tick. */
        MONTH,

        /** Three months per tick. */
        SEASON,

        /** One year per tick. */
        YEAR,

        /** Ten years per tick. */
        DECADE,

        /** One hundred years per tick. */
        CENTURY
    }

    public enum SimulationState {
        /** Simulation created but not yet started. */
        CREATED,

        /** Simulation is actively running. */
        RUNNING,

        /** Simulation is paused. */
        PAUSED,

        /** Simulation has reached end year. */
        COMPLETED,

        /** Simulation encountered an error. */
        ERROR
    }

    // Constructors

    public Simulation() {
    }

    public Simulation(String name, WorldMap worldMap, Integer startYear) {
        this.name = name;
        this.worldMap = worldMap;
        this.startYear = startYear;
        this.currentYear = startYear;
    }

    // Business logic

    /**
     * Advances simulation time by one time step.
     */
    public void advanceTime() {
        switch (timeStep) {
            case MONTH -> {
                currentMonth++;
                if (currentMonth >= 12) {
                    currentMonth = 0;
                    currentYear++;
                }
            }
            case SEASON -> {
                currentMonth += 3;
                if (currentMonth >= 12) {
                    currentMonth = 0;
                    currentYear++;
                }
            }
            case YEAR -> currentYear++;
            case DECADE -> currentYear += 10;
            case CENTURY -> currentYear += 100;
        }
        totalTicks++;
        lastTickAt = LocalDateTime.now();
    }

    /**
     * Gets formatted date string (e.g., "5000 BC, Month 6").
     */
    public String getFormattedDate() {
        int displayYear = Math.abs(currentYear);
        String era = currentYear < 0 ? "BC" : "AD";
        return String.format("%d %s, Month %d", displayYear, era, currentMonth + 1);
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public WorldMap getWorldMap() {
        return worldMap;
    }

    public void setWorldMap(WorldMap worldMap) {
        this.worldMap = worldMap;
    }

    public Integer getStartYear() {
        return startYear;
    }

    public void setStartYear(Integer startYear) {
        this.startYear = startYear;
    }

    public Integer getCurrentYear() {
        return currentYear;
    }

    public void setCurrentYear(Integer currentYear) {
        this.currentYear = currentYear;
    }

    public Integer getCurrentMonth() {
        return currentMonth;
    }

    public void setCurrentMonth(Integer currentMonth) {
        this.currentMonth = currentMonth;
    }

    public TimeStep getTimeStep() {
        return timeStep;
    }

    public void setTimeStep(TimeStep timeStep) {
        this.timeStep = timeStep;
    }

    public Long getTotalTicks() {
        return totalTicks;
    }

    public void setTotalTicks(Long totalTicks) {
        this.totalTicks = totalTicks;
    }

    public SimulationState getState() {
        return state;
    }

    public void setState(SimulationState state) {
        this.state = state;
    }

    public Long getTotalPopulation() {
        return totalPopulation;
    }

    public void setTotalPopulation(Long totalPopulation) {
        this.totalPopulation = totalPopulation;
    }

    public Double getGlobalTemperatureOffset() {
        return globalTemperatureOffset;
    }

    public void setGlobalTemperatureOffset(Double offset) {
        this.globalTemperatureOffset = offset;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getLastTickAt() {
        return lastTickAt;
    }
}


