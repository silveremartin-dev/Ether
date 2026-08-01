package org.ether.society.model;

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
public class Nation {
    private final String id;
    private String name;
    private Color color;
    private H3Cell capital;
    private final Set<H3Cell> territory = new HashSet<>();

    // Cliodynamics & Institutional Indicators (Turchin Secular Cycles)
    private double asabiyyah = 0.8;             // Social cohesion & solidarity (0.0 to 1.0)
    private double eliteOverproduction = 0.15;  // Intra-elite competition & inequality pressure (0.0 to 1.0)
    private double stateCapacity = 0.50;        // Institutional extraction & governance efficiency (0.0 to 1.0)
    private double politicalInstability = 0.10; // Instability & civil rebellion risk index (0.0 to 1.0)

    public Nation(String name, Color color, H3Cell capital) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.color = color;
        this.capital = capital;
        if (capital != null) {
            addCell(capital);
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public H3Cell getCapital() {
        return capital;
    }

    public void setCapital(H3Cell capital) {
        this.capital = capital;
        if (capital != null) {
            addCell(capital); // Ensure capital is owned
        }
    }

    public void addCell(H3Cell cell) {
        if (cell != null && territory.add(cell)) {
            cell.setOwner(this); // Assuming H3Cell has setOwner
        }
    }

    public void removeCell(H3Cell cell) {
        if (territory.remove(cell)) {
            if (cell.getOwner() == this) {
                cell.setOwner(null);
            }
        }
    }

    public Set<H3Cell> getTerritory() {
        return Collections.unmodifiableSet(territory);
    }

    public long getTotalPopulation() {
        return territory.stream().mapToLong(H3Cell::getPopulation).sum();
    }

    public double getAsabiyyah() {
        return asabiyyah;
    }

    public void setAsabiyyah(double asabiyyah) {
        this.asabiyyah = Math.clamp(asabiyyah, 0.0, 1.0);
    }

    public double getEliteOverproduction() {
        return eliteOverproduction;
    }

    public void setEliteOverproduction(double eliteOverproduction) {
        this.eliteOverproduction = Math.clamp(eliteOverproduction, 0.0, 1.0);
    }

    public double getStateCapacity() {
        return stateCapacity;
    }

    public void setStateCapacity(double stateCapacity) {
        this.stateCapacity = Math.clamp(stateCapacity, 0.0, 1.0);
    }

    public double getPoliticalInstability() {
        return politicalInstability;
    }

    public void setPoliticalInstability(double politicalInstability) {
        this.politicalInstability = Math.clamp(politicalInstability, 0.0, 1.0);
    }

    /**
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
