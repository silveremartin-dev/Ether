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

    // Future: Relationships, Stats, etc.

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
}
