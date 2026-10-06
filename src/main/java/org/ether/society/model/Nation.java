package org.ether.society.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ether.society.database.H3Cell;
import javafx.scene.paint.Color;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a political entity (Tribe, Nation, Empire) that controls
 * territory.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * <h1>Nation</h1>
 * <p>
 * Core operational component for the Ether civilizational and planetary simulation framework.<br>
 * Integrates cellular dynamics, data structures, and deterministic state transitions.
 * </p>
 * 
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class Nation {
    /* Internal state variable for id (String). */
    private final String id;
    /* Internal state variable for name (String). */
    private String name;
    @JsonIgnore
    private Color color;
    @JsonIgnore
    private H3Cell capital;
    @JsonIgnore
    private final Set<H3Cell> territory = new HashSet<>();

    // Cliodynamics & Institutional Indicators (Turchin Secular Cycles)
    private double asabiyyah = 0.8;             // Social cohesion & solidarity (0.0 to 1.0)
    private double eliteOverproduction = 0.15;  // Intra-elite competition & inequality pressure (0.0 to 1.0)
    private double stateCapacity = 0.50;        // Institutional extraction & governance efficiency (0.0 to 1.0)
    private double politicalInstability = 0.10; // Instability & civil rebellion risk index (0.0 to 1.0)

    /*
     * Nation.
     * Enforces physical invariants and updates associated state variables within {@code Nation}.
     *
     * @param name the name parameter (String)
     * @param color the color parameter (Color)
     * @param capital the capital parameter (H3Cell)
     */
    public Nation(String name, Color color, H3Cell capital) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.color = color;
        this.capital = capital;
        if (capital != null) {
            addCell(capital);
        }
    }

    /*
     * Get id.
     * Enforces physical invariants and updates associated state variables within {@code Nation}.
     *
     * @return the resulting computation or state reference
     */
    public String getId() {
        return id;
    }

    /*
     * Get name.
     * Enforces physical invariants and updates associated state variables within {@code Nation}.
     *
     * @return the resulting computation or state reference
     */
    public String getName() {
        return name;
    }

    /*
     * Set name.
     * Enforces physical invariants and updates associated state variables within {@code Nation}.
     *
     * @param name the name parameter (String)
     */
    public void setName(String name) {
        this.name = name;
    }

    @JsonIgnore
    /*
     * Get color.
     * Enforces physical invariants and updates associated state variables within {@code Nation}.
     *
     * @return the resulting computation or state reference
     */
    public Color getColor() {
        return color;
    }

    @JsonIgnore
    /*
     * Set color.
     * Enforces physical invariants and updates associated state variables within {@code Nation}.
     *
     * @param color the color parameter (Color)
     */
    public void setColor(Color color) {
        this.color = color;
    }

    @JsonIgnore
    /*
     * Get capital.
     * Enforces physical invariants and updates associated state variables within {@code Nation}.
     *
     * @return the resulting computation or state reference
     */
    public H3Cell getCapital() {
        return capital;
    }

    @JsonIgnore
    /*
     * Set capital.
     * Enforces physical invariants and updates associated state variables within {@code Nation}.
     *
     * @param capital the capital parameter (H3Cell)
     */
    public void setCapital(H3Cell capital) {
        this.capital = capital;
        if (capital != null) {
            addCell(capital); // Ensure capital is owned
        }
    }

    /*
     * Add cell.
     * Enforces physical invariants and updates associated state variables within {@code Nation}.
     *
     * @param cell the cell parameter (H3Cell)
     */
    public void addCell(H3Cell cell) {
        if (cell != null && territory.add(cell)) {
            cell.setOwner(this); // Assuming H3Cell has setOwner
        }
    }

    /*
     * Remove cell.
     * Enforces physical invariants and updates associated state variables within {@code Nation}.
     *
     * @param cell the cell parameter (H3Cell)
     */
    public void removeCell(H3Cell cell) {
        if (territory.remove(cell)) {
            if (cell.getOwner() == this) {
                cell.setOwner(null);
            }
        }
    }

    @JsonIgnore
    /*
     * Get territory.
     * Enforces physical invariants and updates associated state variables within {@code Nation}.
     *
     * @return the resulting computation or state reference
     */
    public Set<H3Cell> getTerritory() {
        return Collections.unmodifiableSet(territory);
    }

    @JsonIgnore
    /*
     * Get total population.
     * Enforces physical invariants and updates associated state variables within {@code Nation}.
     *
     * @return the resulting computation or state reference
     */
    public long getTotalPopulation() {
        return territory.stream().mapToLong(H3Cell::getPopulation).sum();
    }

    /*
     * Get asabiyyah.
     * Enforces physical invariants and updates associated state variables within {@code Nation}.
     *
     * @return the resulting computation or state reference
     */
    public double getAsabiyyah() {
        return asabiyyah;
    }

    /*
     * Set asabiyyah.
     * Enforces physical invariants and updates associated state variables within {@code Nation}.
     *
     * @param asabiyyah the asabiyyah parameter (double)
     */
    public void setAsabiyyah(double asabiyyah) {
        this.asabiyyah = Math.clamp(asabiyyah, 0.0, 1.0);
    }

    /*
     * Get elite overproduction.
     * Enforces physical invariants and updates associated state variables within {@code Nation}.
     *
     * @return the resulting computation or state reference
     */
    public double getEliteOverproduction() {
        return eliteOverproduction;
    }

    /*
     * Set elite overproduction.
     * Enforces physical invariants and updates associated state variables within {@code Nation}.
     *
     * @param eliteOverproduction the elite overproduction parameter (double)
     */
    public void setEliteOverproduction(double eliteOverproduction) {
        this.eliteOverproduction = Math.clamp(eliteOverproduction, 0.0, 1.0);
    }

    /*
     * Get state capacity.
     * Enforces physical invariants and updates associated state variables within {@code Nation}.
     *
     * @return the resulting computation or state reference
     */
    public double getStateCapacity() {
        return stateCapacity;
    }

    /*
     * Set state capacity.
     * Enforces physical invariants and updates associated state variables within {@code Nation}.
     *
     * @param stateCapacity the state capacity parameter (double)
     */
    public void setStateCapacity(double stateCapacity) {
        this.stateCapacity = Math.clamp(stateCapacity, 0.0, 1.0);
    }

    /*
     * Get political instability.
     * Enforces physical invariants and updates associated state variables within {@code Nation}.
     *
     * @return the resulting computation or state reference
     */
    public double getPoliticalInstability() {
        return politicalInstability;
    }

    /*
     * Set political instability.
     * Enforces physical invariants and updates associated state variables within {@code Nation}.
     *
     * @param politicalInstability the political instability parameter (double)
     */
    public void setPoliticalInstability(double politicalInstability) {
        this.politicalInstability = Math.clamp(politicalInstability, 0.0, 1.0);
    }

    /*
     * Updates Peter Turchin's Secular Cycle indices (Asabiyyah, Elite Overproduction, Political Instability)
     * based on territorial size, average Gini coefficient and population density.
     */
    public void updateCliodynamicsCycle() {
        if (territory.isEmpty()) return;

        double avgGini = territory.stream().mapToDouble(H3Cell::getGiniIndex).average().orElse(0.35);
        long totalPop = getTotalPopulation();

        // 1. Asabiyyah decays with large imperial size and high inequality, but grows during existential hardship
        double sizePenalty = Math.log10(Math.max(1, territory.size())) * 0.02;
        double inequalityPenalty = avgGini * 0.05;
        this.asabiyyah = Math.clamp(this.asabiyyah - sizePenalty - inequalityPenalty + 0.01, 0.05, 1.0);

        // 2. Elite Overproduction increases with total wealth and high Gini inequality
        this.eliteOverproduction = Math.clamp(avgGini * 1.2 + (totalPop > 1_000_000 ? 0.2 : 0.0), 0.0, 1.0);

        // 3. State Capacity scales with administrative stability and Asabiyyah
        this.stateCapacity = Math.clamp((0.4 * asabiyyah) + (0.6 * (1.0 - eliteOverproduction)), 0.1, 1.0);

        // 4. Political Instability Index (PSI) formulation: PSI = (1 - Asabiyyah) * EliteOverproduction * (1 + Gini)
        this.politicalInstability = Math.clamp((1.0 - asabiyyah) * (0.5 + eliteOverproduction) * (1.0 + avgGini), 0.0, 1.0);
    }
}
