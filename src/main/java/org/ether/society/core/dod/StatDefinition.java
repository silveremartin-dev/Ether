/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.dod;

/**
 * Metadata definition and mathematical expression specification for pluggable statistics.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class StatDefinition {
    private String id;
    private String name;
    private String category;
    private String expression;
    private String unit;
    private String description;
    private boolean builtin;

    public StatDefinition() {}

    public StatDefinition(String id, String name, String category, String expression, String unit, String description, boolean builtin) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.expression = expression;
        this.unit = unit;
        this.description = description;
        this.builtin = builtin;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getExpression() { return expression; }
    public void setExpression(String expression) { this.expression = expression; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isBuiltin() { return builtin; }
    public void setBuiltin(boolean builtin) { this.builtin = builtin; }
}
