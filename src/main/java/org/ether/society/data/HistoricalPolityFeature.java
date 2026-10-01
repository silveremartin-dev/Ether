/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import java.awt.Color;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

public class HistoricalPolityFeature {
    public String name;
    public int fromYear;
    public int toYear;
    public String seshatId;
    public String wikipedia;
    public List<Path2D> paths = new ArrayList<>();
    public Color color;
}
