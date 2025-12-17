package org.ether.society.culture;

import java.util.Random;

/**
 * Represents the cultural signature of a population.
 * Uses a 3-dimensional vector (RGB) for visualization and distance calculation.
 * 
 * Axes (Abstract):
 * - Red: Aggression / Expansionism
 * - Green: Commerce / Collectivism
 * - Blue: Spirituality / Tradition
 */
public class CultureVector {
    private final double red;
    private final double green;
    private final double blue;

    public static final CultureVector NEUTRAL = new CultureVector(0.5, 0.5, 0.5);

    public CultureVector(double red, double green, double blue) {
        this.red = clamp(red);
        this.green = clamp(green);
        this.blue = clamp(blue);
    }

    public static CultureVector random() {
        Random rng = new Random();
        return new CultureVector(rng.nextDouble(), rng.nextDouble(), rng.nextDouble());
    }

    public double getRed() {
        return red;
    }

    public double getGreen() {
        return green;
    }

    public double getBlue() {
        return blue;
    }

    /**
     * Euclidean distance between two vectors.
     * Range: 0.0 to ~1.73 (sqrt(3))
     */
    public double distance(CultureVector other) {
        if (other == null)
            return 1.0;
        double dr = this.red - other.red;
        double dg = this.green - other.green;
        double db = this.blue - other.blue;
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }

    /**
     * Blend this vector with another.
     * 
     * @param other The other vector
     * @param ratio Blending strength (0.0 = keep self, 1.0 = become other)
     */
    public CultureVector blend(CultureVector other, double ratio) {
        if (other == null)
            return this;
        double inv = 1.0 - ratio;
        return new CultureVector(
                this.red * inv + other.red * ratio,
                this.green * inv + other.green * ratio,
                this.blue * inv + other.blue * ratio);
    }

    /**
     * Apply random drift.
     * 
     * @param magnitude Amount of potential change (e.g., 0.01)
     */
    public CultureVector drift(double magnitude) {
        Random rng = new Random();
        double dr = (rng.nextDouble() - 0.5) * magnitude;
        double dg = (rng.nextDouble() - 0.5) * magnitude;
        double db = (rng.nextDouble() - 0.5) * magnitude;
        return new CultureVector(red + dr, green + dg, blue + db);
    }

    private double clamp(double val) {
        return Math.max(0.0, Math.min(1.0, val));
    }

    @Override
    public String toString() {
        return String.format("[R:%.2f, G:%.2f, B:%.2f]", red, green, blue);
    }
}
