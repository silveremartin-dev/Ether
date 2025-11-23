package com.ether.society.model;

public class ClimateSystem {
    
    public void update(World world, int month) {
        int width = world.getWidth();
        int height = world.getHeight();
        
        // 0 = Jan (Winter in North), 6 = July (Summer in North)
        // Season factor: -1.0 (Winter) to 1.0 (Summer)
        // In Northern Hemisphere: Jan is cold (-1), July is hot (1)
        // In Southern Hemisphere: Jan is hot (1), July is cold (-1)
        
        double seasonPhase = Math.cos((month / 12.0) * 2.0 * Math.PI); // 1 at Jan (Wait, cos(0)=1). 
        // Let's align: Jan (0) should be Winter in North.
        // cos(0) = 1. cos(pi) = -1.
        // If we want Jan to be cold, we want -1. So -cos(...)
        
        double globalSeasonFactor = -Math.cos((month / 12.0) * 2.0 * Math.PI); 
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Cell cell = world.getCell(x, y);
                if (cell == null) continue;
                
                // Determine hemisphere (0 to height)
                // 0 = North Pole, height = South Pole
                double latitude = (double)y / height; // 0.0 to 1.0
                
                double localSeasonFactor;
                if (latitude < 0.5) {
                    // Northern Hemisphere
                    localSeasonFactor = globalSeasonFactor; 
                } else {
                    // Southern Hemisphere
                    localSeasonFactor = -globalSeasonFactor;
                }
                
                // Apply temp change
                // Base temp is already set. We add seasonal variance.
                // Variance is stronger at poles, weaker at equator.
                double latitudeVariance = Math.abs(latitude - 0.5) * 2.0; // 0 at equator, 1 at poles
                
                double seasonalTempChange = localSeasonFactor * 15.0 * latitudeVariance; // +/- 15 degrees at poles
                
                // We don't overwrite base temp, we might need to store current temp separately or calculate it on fly.
                // For now, let's assume Cell has 'currentTemperature' and 'baseTemperature'.
                // But Cell only has 'temperature'. 
                // Let's assume the temperature in Cell IS the current temperature.
                // But if we modify it every tick, it will drift.
                // We need to store base temp in Cell or recalculate it.
                // Let's recalculate base temp here to be safe, or add baseTemp to Cell.
                // For simplicity, I'll recalculate base temp using the same logic as TerrainGenerator.
                
                double elevation = cell.getElevation();
                double latitudeFactor = 1.0 - Math.abs(y - height/2.0) / (height/2.0); 
                double baseTemp = (latitudeFactor * 40.0) - 10.0; 
                baseTemp -= elevation * 10.0; 
                
                double currentTemp = baseTemp + seasonalTempChange + world.getGlobalTemperatureOffset();
                cell.setTemperature(currentTemp);
            }
        }
    }
}
