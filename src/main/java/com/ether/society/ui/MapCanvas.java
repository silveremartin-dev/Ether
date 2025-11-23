package com.ether.society.ui;

import com.ether.society.model.Biome;
import com.ether.society.model.Cell;
import com.ether.society.model.World;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class MapCanvas extends Canvas {
    private static final int CELL_SIZE = 5; // Pixels per cell
    private World world;

    public MapCanvas(World world) {
        this.world = world;
        setWidth(world.getWidth() * CELL_SIZE);
        setHeight(world.getHeight() * CELL_SIZE);
    }

    public void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());

        for (int x = 0; x < world.getWidth(); x++) {
            for (int y = 0; y < world.getHeight(); y++) {
                Cell cell = world.getCell(x, y);
                gc.setFill(getColorForBiome(cell.getBiome()));
                gc.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                
                // Draw agents (simple dot)
                if (cell.getHumanCount() > 0) {
                    gc.setFill(Color.RED);
                    gc.fillOval(x * CELL_SIZE + 1, y * CELL_SIZE + 1, CELL_SIZE - 2, CELL_SIZE - 2);
                }
            }
        }
    }

    private Color getColorForBiome(Biome biome) {
        switch (biome) {
            case OCEAN: return Color.web("#1a237e"); // Deep Blue
            case BEACH: return Color.web("#fff176"); // Sand
            case PLAINS: return Color.web("#81c784"); // Light Green
            case FOREST: return Color.web("#2e7d32"); // Dark Green
            case JUNGLE: return Color.web("#1b5e20"); // Jungle Green
            case DESERT: return Color.web("#fdd835"); // Yellow
            case HILLS: return Color.web("#795548"); // Brown
            case MOUNTAINS: return Color.web("#424242"); // Grey
            case SNOW: return Color.web("#e0e0e0"); // White
            case TUNDRA: return Color.web("#b0bec5"); // Blue Grey
            default: return Color.BLACK;
        }
    }
    
    public void setWorld(World world) {
        this.world = world;
        setWidth(world.getWidth() * CELL_SIZE);
        setHeight(world.getHeight() * CELL_SIZE);
        draw();
    }
    
    public void setOnCellHover(java.util.function.Consumer<Cell> onHover) {
        setOnMouseMoved(e -> {
            int x = (int) (e.getX() / CELL_SIZE);
            int y = (int) (e.getY() / CELL_SIZE);
            Cell cell = world.getCell(x, y);
            if (cell != null) {
                onHover.accept(cell);
            }
        });
    }
}
