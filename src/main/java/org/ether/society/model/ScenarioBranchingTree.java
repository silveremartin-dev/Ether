/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.model;

import org.ether.society.database.H3Cell;

import java.io.Serializable;
import java.util.*;

/**
 * Scenario Multiverse Branching Tree.
 * Allows snapshotting simulation states at year T and branching into alternate physical trajectories
 * for side-by-side comparative telemetry analysis.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class ScenarioBranchingTree implements Serializable {

    public static class SimulationBranch implements Serializable {
        private final String id;
        private final String name;
        private final long parentBranchYear;
        private final List<H3Cell> snapshotCells;
        private final Map<Long, Double> yearToTotalPopulation = new LinkedHashMap<>();
        private final Map<Long, Double> yearToAverageTemperature = new LinkedHashMap<>();

        public SimulationBranch(String id, String name, long parentBranchYear, List<H3Cell> snapshotCells) {
            this.id = id;
            this.name = name;
            this.parentBranchYear = parentBranchYear;
            this.snapshotCells = snapshotCells != null ? new ArrayList<>(snapshotCells) : new ArrayList<>();
        }

        public void recordTelemetry(long year, double totalPop, double avgTemp) {
            yearToTotalPopulation.put(year, totalPop);
            yearToAverageTemperature.put(year, avgTemp);
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public long getParentBranchYear() { return parentBranchYear; }
        public List<H3Cell> getSnapshotCells() { return snapshotCells; }
        public Map<Long, Double> getYearToTotalPopulation() { return yearToTotalPopulation; }
        public Map<Long, Double> getYearToAverageTemperature() { return yearToAverageTemperature; }
    }

    private final Map<String, SimulationBranch> branches = new LinkedHashMap<>();
    private String activeBranchId = "main";

    public ScenarioBranchingTree() {
        // Root Main Branch
        branches.put("main", new SimulationBranch("main", "Trajectoire Principale (Main)", 0, Collections.emptyList()));
    }

    public SimulationBranch createBranch(String branchName, long currentYear, List<H3Cell> cells) {
        String newId = "branch_" + System.currentTimeMillis();
        SimulationBranch branch = new SimulationBranch(newId, branchName, currentYear, cells);
        branches.put(newId, branch);
        return branch;
    }

    public Map<String, SimulationBranch> getBranches() { return Collections.unmodifiableMap(branches); }
    public String getActiveBranchId() { return activeBranchId; }
    public void setActiveBranchId(String id) { if (branches.containsKey(id)) this.activeBranchId = id; }
}

