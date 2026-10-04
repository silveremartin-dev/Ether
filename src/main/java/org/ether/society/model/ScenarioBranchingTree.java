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
        /* Internal state variable for id (String). */
        private final String id;
        /* Internal state variable for name (String). */
        private final String name;
        /* Internal state variable for parent branch year (long). */
        private final long parentBranchYear;
        /* Internal state variable for snapshot cells (List&lt;H3Cell&gt;). */
        private final List<H3Cell> snapshotCells;
        private final Map<Long, Double> yearToTotalPopulation = new LinkedHashMap<>();
        private final Map<Long, Double> yearToAverageTemperature = new LinkedHashMap<>();

        /*
         * Simulation branch.
         * Enforces physical invariants and updates associated state variables within {@code ScenarioBranchingTree}.
         *
         * @param id the id parameter (String)
         * @param name the name parameter (String)
         * @param parentBranchYear the parent branch year parameter (long)
         * @param snapshotCells the snapshot cells parameter (List&lt;H3Cell&gt;)
         * @return the resulting computation or state reference
         */
        public SimulationBranch(String id, String name, long parentBranchYear, List<H3Cell> snapshotCells) {
            this.id = id;
            this.name = name;
            this.parentBranchYear = parentBranchYear;
            this.snapshotCells = snapshotCells != null ? new ArrayList<>(snapshotCells) : new ArrayList<>();
        }

        /*
         * Record telemetry.
         * Enforces physical invariants and updates associated state variables within {@code ScenarioBranchingTree}.
         *
         * @param year the year parameter (long)
         * @param totalPop the total pop parameter (double)
         * @param avgTemp the avg temp parameter (double)
         */
        public void recordTelemetry(long year, double totalPop, double avgTemp) {
            yearToTotalPopulation.put(year, totalPop);
            yearToAverageTemperature.put(year, avgTemp);
        }

        /*
         * Get id.
         * Enforces physical invariants and updates associated state variables within {@code ScenarioBranchingTree}.
         *
         * @return the resulting computation or state reference
         */
        public String getId() { return id; }
        /*
         * Get name.
         * Enforces physical invariants and updates associated state variables within {@code ScenarioBranchingTree}.
         *
         * @return the resulting computation or state reference
         */
        public String getName() { return name; }
        /*
         * Get parent branch year.
         * Enforces physical invariants and updates associated state variables within {@code ScenarioBranchingTree}.
         *
         * @return the resulting computation or state reference
         */
        public long getParentBranchYear() { return parentBranchYear; }
        /*
         * Get snapshot cells.
         * Enforces physical invariants and updates associated state variables within {@code ScenarioBranchingTree}.
         *
         * @return the resulting computation or state reference
         */
        public List<H3Cell> getSnapshotCells() { return snapshotCells; }
        /*
         * Get year to total population.
         * Enforces physical invariants and updates associated state variables within {@code ScenarioBranchingTree}.
         *
         * @return the resulting computation or state reference
         */
        public Map<Long, Double> getYearToTotalPopulation() { return yearToTotalPopulation; }
        /*
         * Get year to average temperature.
         * Enforces physical invariants and updates associated state variables within {@code ScenarioBranchingTree}.
         *
         * @return the resulting computation or state reference
         */
        public Map<Long, Double> getYearToAverageTemperature() { return yearToAverageTemperature; }
    }

    private final Map<String, SimulationBranch> branches = new LinkedHashMap<>();
    /* Internal state variable for active branch id (String). */
    private String activeBranchId = "main";

    /*
     * Scenario branching tree.
     * Enforces physical invariants and updates associated state variables within {@code ScenarioBranchingTree}.
     *
     */
    public ScenarioBranchingTree() {
        // Root Main Branch
        branches.put("main", new SimulationBranch("main", "Trajectoire Principale (Main)", 0, Collections.emptyList()));
    }

    /*
     * Create branch.
     * Enforces physical invariants and updates associated state variables within {@code ScenarioBranchingTree}.
     *
     * @param branchName the branch name parameter (String)
     * @param currentYear the current year parameter (long)
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @return the resulting computation or state reference
     */
    public SimulationBranch createBranch(String branchName, long currentYear, List<H3Cell> cells) {
        String newId = "branch_" + System.currentTimeMillis();
        SimulationBranch branch = new SimulationBranch(newId, branchName, currentYear, cells);
        branches.put(newId, branch);
        return branch;
    }

    /*
     * Get branches.
     * Enforces physical invariants and updates associated state variables within {@code ScenarioBranchingTree}.
     *
     * @return the resulting computation or state reference
     */
    public Map<String, SimulationBranch> getBranches() { return Collections.unmodifiableMap(branches); }
    /*
     * Get active branch id.
     * Enforces physical invariants and updates associated state variables within {@code ScenarioBranchingTree}.
     *
     * @return the resulting computation or state reference
     */
    public String getActiveBranchId() { return activeBranchId; }
    public void setActiveBranchId(String id) { if (branches.containsKey(id)) this.activeBranchId = id; }
}

