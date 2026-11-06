/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.model.Scenario;

/**
 * Biophysical niche suitability, paleoclimatic orbital insolation, megafaunal extinction
 * coupling, and regional historical geography helper for {@link ProceduralPopulationEngine}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see ProceduralPopulationEngine
 */
final class ProceduralPopulationSuitabilityHelper {

    private ProceduralPopulationSuitabilityHelper() {
        // Static helper class
    }

    /**
     * Regional weighting for Earth historical geography based on technological era and start year.
     *
     * @param lat       cell latitude in degrees
     * @param lng       cell longitude in degrees
     * @param biome     cell biome classification
     * @param techLevel technological level
     * @param startYear scenario start year (negative BCE/BP, positive CE)
     * @param scenario  scenario configuration
     * @return regional suitability weight
     */
    static double getEarthHistoricalRegionalWeight(double lat, double lng, Biome biome,
                                                   double techLevel, long startYear, Scenario scenario) {
        boolean isEastAfrica = (lat >= -15.0 && lat <= 15.0) && (lng >= 25.0 && lng <= 45.0);
        boolean isFertileCrescent = (lat >= 25.0 && lat <= 40.0) && (lng >= 32.0 && lng <= 50.0);
        boolean isNileDelta = (lat >= 15.0 && lat <= 35.0) && (lng >= 22.0 && lng <= 38.0);
        boolean isIndusValley = (lat >= 20.0 && lat <= 35.0) && (lng >= 65.0 && lng <= 78.0);
        boolean isYellowYangtzeChina = (lat >= 20.0 && lat <= 42.0) && (lng >= 100.0 && lng <= 125.0);
        boolean isGangesIndia = (lat >= 8.0 && lat <= 30.0) && (lng >= 70.0 && lng <= 90.0);
        boolean isMediterraneanEurope = (lat >= 25.0 && lat <= 58.0) && (lng >= -12.0 && lng <= 45.0);
        boolean isMesoamerica = (lat >= 10.0 && lat <= 25.0) && (lng >= -110.0 && lng <= -80.0);
        boolean isAndes = (lat >= -25.0 && lat <= 5.0) && (lng >= -82.0 && lng <= -62.0);
        boolean isSahulAustralia = (lat < 10.0 && lng > 95.0) || (lat < -8.0 && lng > 110.0);
        boolean isAmericas = lng < -25.0;
        boolean isSahara = (lat >= 10.0 && lat <= 32.0) && (lng >= -18.0 && lng <= 35.0);

        if (scenario != null && scenario.getPopulationDensityType() != null) {
            String pType = scenario.getPopulationDensityType().toUpperCase();
            switch (pType) {
                case "AUSTRALIA_SAHUL" -> { return isSahulAustralia ? 15.0 : 0.0; }
                case "EGYPT_NILE" -> { return isNileDelta ? 20.0 : 0.0; }
                case "MESOAMERICA" -> { return isMesoamerica ? 20.0 : 0.0; }
                case "ROMAN_EMPIRE" -> { return isMediterraneanEurope ? 20.0 : 0.0; }
                case "WEST_AFRICA_MALI" -> { return (lat >= 5.0 && lat <= 25.0 && lng >= -18.0 && lng <= 15.0) ? 20.0 : 0.0; }
                case "JAPAN_SAKOKU" -> { return (lat >= 30.0 && lat <= 45.0 && lng >= 128.0 && lng <= 146.0) ? 20.0 : 0.0; }
                case "INDIA_MAURYA" -> { return (isGangesIndia || isIndusValley) ? 20.0 : 0.0; }
                case "AMERICAS_1491" -> { return isAmericas ? 15.0 : 0.0; }
                case "BERINGIA_AMERICAS" -> { return (lat >= 50.0 && lat <= 75.0 && (lng >= 140.0 || lng <= -140.0)) ? 20.0 : 0.0; }
                case "GREEN_SAHARA" -> { return isSahara ? 20.0 : 0.0; }
                case "YOUNGER_DRYAS" -> { return (lat >= 28.0 && lat <= 40.0 && lng >= 28.0 && lng <= 45.0) ? 20.0 : 0.0; }
            }
        }

        String scName = scenario != null && scenario.getName() != null ? scenario.getName().toLowerCase() : "";

        // 1. Deep Paleolithic / Out of Africa (~100,000 BP to ~50,000 BP)
        if (startYear < -50000 || (scName.contains("out_of_africa") || (scName.contains("sortie d'afrique") && !scName.contains("sahul")))) {
            if (isAmericas || isSahulAustralia) return 0.0;
            if (isEastAfrica) return 15.0;
            if (isNileDelta || isFertileCrescent) return 6.0;
            if (lat >= -35.0 && lat <= 37.0 && lng >= -18.0 && lng <= 51.0) return 3.0;
            if (lat >= 10.0 && lat <= 55.0 && lng >= -10.0 && lng <= 100.0) return 1.5;
            return 0.0;
        }

        // 2. Sahul Migration & Upper Paleolithic (~50,000 BP to ~25,000 BP)
        if (startYear <= -25000) {
            if (isAmericas) return 0.0;
            if (isSahulAustralia) return 4.0;
            if (isEastAfrica) return 6.0;
            if (isFertileCrescent || isNileDelta) return 5.0;
            if (isYellowYangtzeChina || isGangesIndia) return 4.0;
            if (isMediterraneanEurope) return 4.0;
            return 1.0;
        }

        // 3. Late Pleistocene / Beringian Crossing into Americas (~25,000 BP to ~10,000 BP)
        if (startYear <= -10000) {
            if (isFertileCrescent || isNileDelta) return 10.0;
            if (isYellowYangtzeChina || isGangesIndia) return 8.0;
            if (isMediterraneanEurope) return 6.0;
            if (isSahulAustralia) return 4.0;
            if (isMesoamerica || isAndes) return 3.0;
            return 1.0;
        }

        // 4. Neolithic Revolution & Early Agriculture (-10000 to -3000 BP)
        if (startYear <= -3000) {
            if (isFertileCrescent) return 15.0;
            if (isYellowYangtzeChina) return 12.0;
            if (isNileDelta) return 10.0;
            if (isIndusValley) return 8.0;
            if (isMesoamerica) return 6.0;
            if (isMediterraneanEurope) return 4.0;
            if (isSahulAustralia) return 2.0;
            return 1.0;
        }

        // 5. Bronze Age & Classical Antiquity (-3000 to 500 CE)
        if (startYear <= 500) {
            if (isYellowYangtzeChina) return 20.0;
            if (isGangesIndia || isIndusValley) return 18.0;
            if (isMediterraneanEurope) return 18.0;
            if (isFertileCrescent || isNileDelta) return 14.0;
            if (isMesoamerica || isAndes) return 5.0;
            return 1.2;
        }

        // 6. Medieval & Early Modern (500 CE to 1800 CE)
        if (startYear <= 1800) {
            if (isYellowYangtzeChina) return 22.0;
            if (isGangesIndia) return 20.0;
            if (isMediterraneanEurope) return 18.0;
            if (isFertileCrescent || isNileDelta) return 10.0;
            if (isMesoamerica || isAndes) return 6.0;
            if (lat >= 30 && lat <= 50 && lng >= -100 && lng <= -70) return 4.0;
            return 1.5;
        }

        // 7. Industrial & Modern (> 1800 CE)
        if (isMediterraneanEurope) return 25.0;
        if (isYellowYangtzeChina) return 24.0;
        if (isGangesIndia) return 22.0;
        if (lat >= 30 && lat <= 48 && lng >= -90 && lng <= -70) return 12.0;
        return 2.5;
    }

    /**
     * Biome and elevation suitability adjusted by technological capacity using the continuous
     * human climate niche model (Xu et al. 2020, PNAS).
     *
     * @param cell      target H3 cell
     * @param techLevel technological index ($0 \le T \le 10$)
     * @return suitability factor $\in [0.0001, 2.0]$
     */
    static double calculateBiomeAndElevSuitability(H3Cell cell, double techLevel) {
        if (Boolean.TRUE.equals(cell.getIsPolder())) {
            return 1.5;
        }
        if (Boolean.TRUE.equals(cell.getHasFloatingInfrastructure())) {
            return 0.8 + Math.min(1.0, techLevel * 0.1);
        }

        double elev = cell.getElevationOr(0.0);
        if (elev < 0.0) {
            return techLevel >= 8.5 ? Math.clamp(0.40 + (techLevel - 8.5) * 0.15, 0.0001, 1.0) : 0.0001;
        }

        double tempC = cell.getTemperatureOr(15.0);
        double rainMm = cell.getRainfallOr(500.0);

        double sigmaT = 6.5 * (1.0 + 0.10 * techLevel);
        double thermalNiche = Math.exp(-Math.pow(tempC - 13.5, 2.0) / (2.0 * sigmaT * sigmaT));
        double moistureNiche = (rainMm / (rainMm + 300.0)) * Math.exp(-rainMm / 4000.0);

        double maxHabitableElev = 500.0 + (techLevel * 300.0);
        double elevFactor;
        if (elev > maxHabitableElev) {
            double overflow = (elev - maxHabitableElev) / 1000.0;
            elevFactor = Math.max(0.001, Math.exp(-overflow * (3.5 - techLevel * 0.25)));
        } else {
            elevFactor = 1.0 - (elev / (maxHabitableElev * 2.0));
        }

        return Math.clamp(thermalNiche * moistureNiche * Math.max(0.01, elevFactor), 0.001, 2.0);
    }

    /**
     * Gaussian thermal comfort suitability centered at $18^\circ\text{C}$ with tech-broadened tolerance $\sigma(T)$.
     *
     * @param temp      local mean temperature in $^\circ\text{C}$
     * @param techLevel technological level
     * @return thermal suitability $\in (0, 1]$
     */
    static double calculateTemperatureSuitability(double temp, double techLevel) {
        double optimalTemp = 18.0;
        double sigma = 8.0 + (techLevel * 2.0);
        double diff = temp - optimalTemp;
        return Math.exp(-(diff * diff) / (2.0 * sigma * sigma));
    }

    /**
     * Calculates planetary solar insolation ($\text{W/m}^2$) dynamically based on celestial mechanics:
     * $$F_{\text{solar}} = \frac{S_0}{a^2} (1 - e^2)^{-1/2} \cos(\phi - \delta)$$
     *
     * @param lat             latitude in degrees
     * @param obliquityDeg    axial obliquity in degrees
     * @param eccentricity    orbital eccentricity
     * @param semiMajorAxisAU semi-major axis in Astronomical Units ($\text{AU}$)
     * @param startYearBP     epoch year
     * @param isEarth         whether to query Earth's exact Milankovitch ephemeris
     * @return top-of-atmosphere solar insolation in $\text{W/m}^2$
     */
    static double calculatePlanetarySolarInsolation(double lat, double obliquityDeg, double eccentricity,
                                                    double semiMajorAxisAU, long startYearBP, boolean isEarth) {
        if (isEarth) {
            org.ether.society.engines.tier1.MilankovitchOrbitalEngine.MilankovitchParameters params =
                    org.ether.society.engines.tier1.MilankovitchOrbitalEngine.computeOrbitalParameters((double) startYearBP);
            return org.ether.society.engines.tier1.MilankovitchOrbitalEngine.calculateDailyInsolation(lat, 172, params);
        } else {
            double solarConstant = 1361.0;
            double distSq = Math.max(0.1, semiMajorAxisAU * semiMajorAxisAU);
            double orbitalCorrection = 1.0 / Math.sqrt(Math.max(0.01, 1.0 - eccentricity * eccentricity));
            double declination = Math.toRadians(obliquityDeg) * Math.sin(2.0 * Math.PI * (startYearBP % 365) / 365.0);
            double latRad = Math.toRadians(lat);
            double cosZenith = Math.max(0.0, Math.sin(latRad) * Math.sin(declination) + Math.cos(latRad) * Math.cos(declination));
            return (solarConstant / distSq) * orbitalCorrection * cosZenith;
        }
    }

    /**
     * Computes Earth's Milankovitch summer insolation at $65^\circ\text{N}$ (Laskar 2004 / Berger 1978).
     *
     * @param startYearBP epoch year
     * @return summer insolation at $65^\circ\text{N}$ in $\text{W/m}^2$
     */
    static double calculateMilankovitchSummerInsolation65N(long startYearBP) {
        double laskar = org.ether.society.analytics.HistoricalValidationKernel.getInterpolatedBenchmarkValue("milankovitchInsolation", (int) startYearBP);
        if (!Double.isNaN(laskar) && laskar > 0.0) {
            return laskar;
        }
        return calculatePlanetarySolarInsolation(65.0, 23.44, 0.0167, 1.0, startYearBP, true);
    }

    /**
     * Calculates plant Net Primary Productivity (NPP) $\text{CO}_2$ fertilization multiplier
     * derived from EPICA Dome C / Vostok ice cores ($180\,\text{ppm}$ LGM peak to $280\,\text{ppm}$ Holocene).
     *
     * @param startYearBP epoch year
     * @return vegetation NPP multiplier
     */
    static double calculateCO2VegetationMultiplier(long startYearBP) {
        double t = (double) Math.abs(startYearBP);
        double co2ppm = 280.0 - 100.0 * Math.exp(-Math.pow((t - 20000.0) / 15000.0, 2.0));
        co2ppm = Math.clamp(co2ppm, 180.0, 280.0);
        return 1.0 + 0.25 * Math.log(co2ppm / 180.0);
    }

    /**
     * Calculates coastal foraging carrying capacity boost ($2.5\times$) for intertidal shellfisheries
     * and marine omega-3 (DHA) resource exploitation along coastal margins.
     *
     * @param cell target H3 cell
     * @return coastal foraging multiplier
     */
    static double calculateCoastalForagingMultiplier(H3Cell cell) {
        if (cell == null) return 1.0;
        Biome b = cell.getBiome();
        double elev = cell.getElevationOr(0.0);
        boolean isCoastal = Boolean.TRUE.equals(cell.getIsCoastal())
                || b == Biome.BEACH
                || (elev > 0.0 && elev <= 50.0 && cell.getCoastalMarineResourceOr(0.0) > 100.0);
        if (isCoastal) {
            double marineBio = cell.getCoastalMarineResourceOr(0.0) + cell.getBiomassFishOr(0.0);
            return 1.0 + 1.5 * Math.tanh(Math.max(marineBio, 200.0) / 400.0);
        }
        return 1.0;
    }

    /**
     * Computes Quaternary Megafauna Extinction (QME) abundance index using Lotka-Volterra dynamics
     * calibrated to Sandom et al. (2014) and Barnosky et al. (2004).
     *
     * @param startYearBP   epoch year
     * @param humanDensity  local human population density
     * @param climateStress climate stress index
     * @return megafauna abundance percentage $\in [1.0, 100.0]$
     */
    static double calculateMegafaunaAbundanceIndex(long startYearBP, double humanDensity, double climateStress) {
        double t = (double) Math.abs(startYearBP);
        if (startYearBP >= -10000) {
            if (startYearBP < 1800) {
                return 9.5 + (20.0 - 9.5) * Math.max(0.0, (1800.0 - startYearBP) / 11800.0);
            } else {
                return Math.max(1.0, 9.5 - (8.0 * Math.min(1.0, (startYearBP - 1800.0) / 226.0)));
            }
        }

        double tKya = t / 1000.0;
        double baselinePleistoceneAbundance = 20.0 + 80.0 / (1.0 + Math.exp((20.0 - tKya) / 8.5));
        double huntingPressure = 0.02 * humanDensity;
        double stressModulation = 1.0 - Math.clamp(huntingPressure + 0.15 * climateStress, 0.0, 0.35);
        return Math.clamp(baselinePleistoceneAbundance * stressModulation, 2.0, 100.0);
    }

    /**
     * Freshwater, river proximity, and accessible aquifer suitability modulated by pumping/aqueduct technology.
     *
     * @param cell      target H3 cell
     * @param techLevel technological index
     * @return hydrological suitability $\in (0, 1]$
     */
    static double calculateWaterAndRiverSuitability(H3Cell cell, double techLevel) {
        double water = cell.getWaterResourceOr(0.0);
        double accessibleAquifer = cell.getAccessibleAquiferOr(0.0);
        Biome b = cell.getBiome();

        double elev = cell.getElevationOr(0.0);
        double rain = cell.getRainfallOr(0.0);
        boolean isCoastal = Boolean.TRUE.equals(cell.getIsCoastal())
                || b == Biome.BEACH
                || (elev >= 0.0 && elev <= 30.0 && cell.getCoastalMarineResourceOr(0.0) > 100.0);
        boolean isRiverValley = (b == Biome.PLAINS && rain > 0.4) || water > 400.0;

        double aquiferScore = Math.min(1.0, accessibleAquifer / 10000.0);
        double baseWaterScore = (water / 1000.0) * 0.45 + (aquiferScore * 0.35) + (isCoastal ? 0.2 : 0.0);
        if (isRiverValley) baseWaterScore += 0.3;

        if (accessibleAquifer > 3000.0 && (b == Biome.DESERT || b == Biome.PLAINS)) {
            baseWaterScore += 0.25;
        }

        baseWaterScore = Math.max(0.01, Math.min(1.0, baseWaterScore));
        double exponent = Math.max(0.4, 3.0 - (techLevel * 0.4));
        return Math.pow(baseWaterScore, exponent);
    }

    /**
     * Resource and carrying capacity suitability bonus from food, wood, and metallic ore stocks.
     *
     * @param cell target H3 cell
     * @return resource suitability multiplier $\in [1.0, 3.0]$
     */
    static double calculateResourceSuitability(H3Cell cell) {
        double food = cell.getFoodResourceOr(0.0);
        double wood = cell.getWoodResourceOr(0.0);
        double metal = cell.getResourceMetalOr(0.0);
        double bonus = 1.0 + (food / 2000.0) + (wood / 3000.0) + (metal / 4000.0);
        return Math.min(3.0, bonus);
    }

    /**
     * Spatial settlement pattern multiplier based on scenario density distribution type.
     *
     * @param cell    target H3 cell
     * @param pattern spatial density pattern identifier
     * @return pattern multiplier
     */
    static double calculatePatternMultiplier(H3Cell cell, String pattern) {
        if (pattern == null) return 1.0;
        double elev = cell.getElevationOr(0.0);
        double lat = cell.getLatitudeOr(0.0);
        double lng = cell.getLongitudeOr(0.0);
        double water = cell.getWaterResourceOr(0.0);
        double aquifer = cell.getAccessibleAquiferOr(0.0);

        return switch (pattern) {
            case "UNBIASED_NATURAL", "UNBIASED", "NONE", "NATURAL_EQUILIBRIUM" -> 1.0;
            case "COASTAL_MARITIME" -> 0.6 + 2.9 / (1.0 + Math.exp(0.05 * (elev - 40.0)));
            case "RIVER_VALLEYS" -> 0.6 + 3.4 / (1.0 + Math.exp(-0.01 * (water - 300.0)));
            case "HIGHLAND_MOUNTAIN" -> 0.6 + 2.9 / (1.0 + Math.exp(-0.005 * (elev - 700.0)));
            case "INLAND_OASIS" -> 0.6 + 3.4 / (1.0 + Math.exp(-0.003 * (aquifer - 1500.0)));
            case "EQUATORIAL_BELT" -> 0.6 + 2.4 * Math.exp(-Math.pow(lat / 18.0, 2.0));
            case "EGYPT_NILE" -> 1.0 + 3.0 * Math.exp(-Math.pow(lat - 27.0, 2.0) / 50.0 - Math.pow(lng - 31.0, 2.0) / 18.0);
            case "GREEN_SAHARA" -> 1.0 + 3.0 * Math.exp(-Math.pow(lat - 22.0, 2.0) / 72.0 - Math.pow(lng - 10.0, 2.0) / 200.0);
            case "MESOAMERICA" -> 1.0 + 2.5 * Math.exp(-Math.pow(lat - 18.0, 2.0) / 32.0 - Math.pow(lng + 95.0, 2.0) / 72.0);
            case "ROMAN_EMPIRE" -> 1.0 + 2.5 * Math.exp(-Math.pow(lat - 38.0, 2.0) / 72.0 - Math.pow(lng - 15.0, 2.0) / 200.0);
            case "AUSTRALIA_SAHUL" -> 1.0 + 2.5 * Math.exp(-Math.pow(lat + 25.0, 2.0) / 100.0 - Math.pow(lng - 135.0, 2.0) / 200.0);
            case "INDIA_MAURYA" -> 1.0 + 3.0 * Math.exp(-Math.pow(lat - 22.0, 2.0) / 50.0 - Math.pow(lng - 78.0, 2.0) / 72.0);
            case "FERTILE_CRESCENT", "MESOPOTAMIA_ASSYRIA" -> 1.0 + 3.0 * Math.exp(-Math.pow(lat - 33.0, 2.0) / 32.0 - Math.pow(lng - 43.0, 2.0) / 50.0);
            case "BERINGIA_AMERICAS", "LGM_REFUGIA", "YOUNGER_DRYAS" -> 1.0 + 2.5 / (1.0 + Math.exp(-0.2 * (lat - 35.0)));
            case "WEST_AFRICA_MALI" -> 1.0 + 2.5 * Math.exp(-Math.pow(lat - 14.0, 2.0) / 32.0 - Math.pow(lng + 4.0, 2.0) / 72.0);
            case "JAPAN_SAKOKU" -> 1.0 + 2.5 * Math.exp(-Math.pow(lat - 36.0, 2.0) / 32.0 - Math.pow(lng - 138.0, 2.0) / 32.0);
            case "AMERICAS_1491", "COLUMBIAN_CONTACT" -> 1.0 + 2.0 * Math.exp(-Math.pow(lng + 75.0, 2.0) / 450.0);
            case "ONE_CONTINENT" -> 1.0 + 2.0 * Math.exp(-Math.pow(lat / 25.0, 2.0));
            case "INDUSTRIAL_1800" -> 1.0 + 2.0 * Math.exp(-Math.pow(lat - 50.0, 2.0) / 72.0);
            case "SPARSE_NOMADIC" -> 0.8;
            default -> 1.0;
        };
    }
}
