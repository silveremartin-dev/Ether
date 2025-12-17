/*
 * MIT License
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.density;

import org.ether.society.database.H3Cell;
import java.util.List;

/**
 * Climate system for H3 cells.
 * Updates temperature based on:
 * - Latitude (equator is warmer)
 * - Season (summer/winter based on hemisphere)
 * - Elevation (lapse rate)
 */
public class H3ClimateSystem {

    // Climate parameters
    private double seasonalVariation = 15.0; // Max Â°C swing from summer to winter
    private double equatorBaseTemp = 30.0; // Base temp at equator
    private double poleBaseTemp = -20.0; // Base temp at poles
    private double elevationLapseRate = 0.006; // Â°C drop per meter

    private org.ether.society.gpu.GPUManager gpuManager;

    public void setGpuManager(org.ether.society.gpu.GPUManager gpuManager) {
        this.gpuManager = gpuManager;
    }

    /**
     * Update temperatures for all cells based on current month.
     */
    public void updateClimate(List<H3Cell> cells, int month) {
        // Calculate Northern Hemisphere Season Base (global assumption for kernel)
        // Peak month = 6 (July)
        int peakSummerMonth = 6;
        int monthsFromPeak = Math.abs(month - peakSummerMonth);
        if (monthsFromPeak > 6)
            monthsFromPeak = 12 - monthsFromPeak;

        double seasonBase = seasonalVariation * Math.cos(monthsFromPeak * Math.PI / 6.0);

        if (gpuManager != null) {
            // Use GPU (or fallback kernel)
            gpuManager.executeClimateKernel(cells, (float) seasonBase);
        } else {
            // Legacy loop (if no manager set)
            for (H3Cell cell : cells) {
                double newTemp = calculateTemperature(
                        cell.getLatitude(),
                        cell.getElevation(),
                        month);
                cell.setTemperature(newTemp);
            }
        }
    }

    /**
     * Calculate temperature for a cell.
     * 
     * @param latitude  Cell latitude (-90 to 90)
     * @param elevation Cell elevation in meters
     * @param month     Current month (0-11)
     * @return Temperature in Celsius
     */
    public double calculateTemperature(double latitude, double elevation, int month) {
        // 1. Base temperature from latitude
        double latFactor = Math.abs(latitude) / 90.0;
        double baseTemp = equatorBaseTemp - latFactor * (equatorBaseTemp - poleBaseTemp);

        // 2. Seasonal variation
        boolean isNorthern = latitude >= 0;
        int peakSummerMonth = isNorthern ? 6 : 0; // July for N, January for S
        int monthsFromPeak = Math.abs(month - peakSummerMonth);
        if (monthsFromPeak > 6)
            monthsFromPeak = 12 - monthsFromPeak;

        // Cosine wave: +seasonalVariation in summer, -seasonalVariation in winter
        double seasonalOffset = seasonalVariation * Math.cos(monthsFromPeak * Math.PI / 6);

        // Seasonal effect is stronger at higher latitudes
        seasonalOffset *= latFactor;

        // 3. Elevation effect (lapse rate: ~6Â°C per 1000m)
        double elevationOffset = -elevation * elevationLapseRate;

        return baseTemp + seasonalOffset + elevationOffset;
    }

    /**
     * Get season name for UI display.
     */
    public String getSeasonName(double latitude, int month) {
        boolean isNorthern = latitude >= 0;

        // Northern hemisphere seasons
        int seasonIdx;
        if (isNorthern) {
            seasonIdx = switch (month) {
                case 11, 0, 1 -> 0; // Winter
                case 2, 3, 4 -> 1; // Spring
                case 5, 6, 7 -> 2; // Summer
                case 8, 9, 10 -> 3; // Autumn
                default -> 0;
            };
        } else {
            // Southern hemisphere - opposite
            seasonIdx = switch (month) {
                case 11, 0, 1 -> 2; // Summer
                case 2, 3, 4 -> 3; // Autumn
                case 5, 6, 7 -> 0; // Winter
                case 8, 9, 10 -> 1; // Spring
                default -> 0;
            };
        }

        return switch (seasonIdx) {
            case 0 -> "Winter";
            case 1 -> "Spring";
            case 2 -> "Summer";
            case 3 -> "Autumn";
            default -> "Unknown";
        };
    }

    // Setters for tuning

    public void setSeasonalVariation(double variation) {
        this.seasonalVariation = variation;
    }

    public void setEquatorBaseTemp(double temp) {
        this.equatorBaseTemp = temp;
    }

    public void setPoleBaseTemp(double temp) {
        this.poleBaseTemp = temp;
    }

    public void setElevationLapseRate(double rate) {
        this.elevationLapseRate = rate;
    }
}
