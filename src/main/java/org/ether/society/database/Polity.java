/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.database;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javafx.scene.paint.Color;

import java.util.*;

/**
 * <p>
 * Represents an autonomous sovereign political entity (Tribe, Chiefdom, City-State, Empire)
 * that administers hexagonal territorial cells.
 * Tracks cliodynamic institutional indicators based on Peter Turchin's Secular Cycles (SDT)
 * and Fisher Monetary Exchange balances.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Polity {
    /** Unique identifier for the polity entity. */
    private final String id;
    /** Name of the sovereign polity. */
    private String name;
    private int argbColor = 0xFF4488EE;
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
    private double treasuryBullion = 0.0;       // Sovereign precious metal bullion treasury (kg of Au/Ag specie)
    private double monetaryInflation = 0.0;     // Bullion-driven inflation / Dutch Disease index (0.0 to 1.0)

    /**
     * Constructs a new sovereign Polity with initial capital.
     *
     * @param name Name of the polity
     * @param color Map visualization color
     * @param capital Capital hexagonal cell
     */
    public Polity(String name, Color color, H3Cell capital) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.color = color;
        if (color != null) {
            int a = (int) Math.round(color.getOpacity() * 255.0);
            int r = (int) Math.round(color.getRed() * 255.0);
            int g = (int) Math.round(color.getGreen() * 255.0);
            int b = (int) Math.round(color.getBlue() * 255.0);
            this.argbColor = (a << 24) | (r << 16) | (g << 8) | b;
        }
        this.capital = capital;
        if (capital != null) {
            addCell(capital);
        }
    }

    /**
     * Constructs a new sovereign Polity with initial capital using 32-bit ARGB integer.
     * Suitable for pure headless execution without JavaFX.
     *
     * @param name Name of the polity
     * @param argbColor 32-bit ARGB color
     * @param capital Capital hexagonal cell
     */
    public Polity(String name, int argbColor, H3Cell capital) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.argbColor = argbColor;
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

    @JsonIgnore
    public Color getColor() {
        if (color == null) {
            int a = (argbColor >>> 24);
            int r = (argbColor >> 16) & 0xFF;
            int g = (argbColor >> 8) & 0xFF;
            int b = argbColor & 0xFF;
            color = Color.rgb(r, g, b, a / 255.0);
        }
        return color;
    }

    @JsonIgnore
    public void setColor(Color color) {
        this.color = color;
        if (color != null) {
            int a = (int) Math.round(color.getOpacity() * 255.0);
            int r = (int) Math.round(color.getRed() * 255.0);
            int g = (int) Math.round(color.getGreen() * 255.0);
            int b = (int) Math.round(color.getBlue() * 255.0);
            this.argbColor = (a << 24) | (r << 16) | (g << 8) | b;
        }
    }

    public int getArgbColor() {
        return argbColor;
    }

    public void setArgbColor(int argbColor) {
        this.argbColor = argbColor;
        this.color = null;
    }

    public String getHexColor() {
        return String.format("#%06X", (0xFFFFFF & argbColor));
    }

    public void setHexColor(String hex) {
        if (hex != null && !hex.isBlank()) {
            try {
                String clean = hex.startsWith("#") ? hex.substring(1) : hex;
                int rgb = Integer.parseInt(clean, 16);
                setArgbColor(0xFF000000 | rgb);
            } catch (Exception ignored) {}
        }
    }

    @JsonIgnore
    public H3Cell getCapital() {
        return capital;
    }

    @JsonIgnore
    public void setCapital(H3Cell capital) {
        this.capital = capital;
        if (capital != null) {
            addCell(capital);
        }
    }

    public void addCell(H3Cell cell) {
        if (cell != null && territory.add(cell)) {
            cell.setOwner(this);
        }
    }

    public void removeCell(H3Cell cell) {
        if (territory.remove(cell)) {
            if (cell.getOwner() == this) {
                cell.setOwner(null);
            }
        }
    }

    @JsonIgnore
    public Set<H3Cell> getTerritory() {
        return Collections.unmodifiableSet(territory);
    }

    @JsonIgnore
    public long getTotalPopulation() {
        return territory.stream().mapToLong(c -> c.getPopulation() != null ? c.getPopulation() : 0).sum();
    }

    // Cliodynamics Getters & Setters

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

    public double getTreasuryBullion() {
        return treasuryBullion;
    }

    public void setTreasuryBullion(double treasuryBullion) {
        this.treasuryBullion = Math.max(0.0, treasuryBullion);
    }

    public double getMonetaryInflation() {
        return monetaryInflation;
    }

    public void setMonetaryInflation(double monetaryInflation) {
        this.monetaryInflation = Math.clamp(monetaryInflation, 0.0, 1.0);
    }

    /**
     * Updates Peter Turchin's Secular Cycle indices (Asabiyyah, Elite Overproduction, Political Instability)
     * and Fisher Monetary Exchange balance (M*V = P*Y) based on territorial size, Gini coefficient,
     * population density, and sovereign bullion reserves.
     */
    public void updateCliodynamicsCycle() {
        if (territory.isEmpty()) return;

        double avgGini = territory.stream().mapToDouble(c -> c.getGiniIndex() != null ? c.getGiniIndex() : 0.35).average().orElse(0.35);
        long totalPop = getTotalPopulation();
        double totalRealCapital = territory.stream()
                .mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0)
                .sum();
        double totalFoodOutput = territory.stream()
                .mapToDouble(c -> c.getFoodResource() != null ? c.getFoodResource() : 0.0)
                .sum();

        // 0. Fisher Equation of Exchange & Dutch Disease (Potosi / 3rd-Century Rome Bullion Inflation):
        double bullionToRealOutputRatio = treasuryBullion / Math.max(10.0, totalRealCapital + totalFoodOutput * 0.5);
        this.monetaryInflation = Math.clamp(Math.max(0.0, bullionToRealOutputRatio - 0.35) * 0.50, 0.0, 1.0);
        double fiscalMonetizationBonus = Math.min(0.15, Math.log1p(treasuryBullion) * 0.02) * (1.0 - monetaryInflation);

        // 1. Asabiyyah decays with large imperial size and high inequality, but grows during existential hardship
        double sizePenalty = Math.log10(Math.max(1, territory.size())) * 0.02;
        double inequalityPenalty = avgGini * 0.05;
        this.asabiyyah = Math.clamp(this.asabiyyah - sizePenalty - inequalityPenalty + 0.01, 0.05, 1.0);

        // 2. Elite Overproduction increases with total wealth, high Gini inequality, and bullion inflation
        this.eliteOverproduction = Math.clamp(avgGini * 1.2 + (totalPop > 1_000_000 ? 0.2 : 0.0) + monetaryInflation * 0.25, 0.0, 1.0);

        // 3. State Capacity scales with administrative stability, Asabiyyah, and monetized tax collection (minus inflation erosion)
        this.stateCapacity = Math.clamp((0.4 * asabiyyah) + (0.6 * (1.0 - eliteOverproduction)) + fiscalMonetizationBonus - (monetaryInflation * 0.20), 0.1, 1.0);

        // 4. Political Instability Index (PSI) formulation: PSI = (1 - Asabiyyah) * EliteOverproduction * (1 + Gini)
        this.politicalInstability = Math.clamp((1.0 - asabiyyah) * (0.5 + eliteOverproduction) * (1.0 + avgGini + monetaryInflation * 0.5), 0.0, 1.0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Polity polity = (Polity) o;
        return Objects.equals(id, polity.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Polity{" +
                "name='" + name + '\'' +
                ", cells=" + territory.size() +
                ", pop=" + getTotalPopulation() +
                ", asabiyyah=" + String.format(Locale.US, "%.2f", asabiyyah) +
                ", instability=" + String.format(Locale.US, "%.2f", politicalInstability) +
                '}';
    }
}