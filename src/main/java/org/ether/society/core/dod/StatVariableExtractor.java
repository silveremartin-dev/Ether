/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.dod;

import org.ether.society.database.H3Cell;
import java.util.List;
import java.util.Locale;

/**
 * High-performance array extractor mapping cliodynamic and biophysical variable keys to memory buffers.
 * <p>
 * Strictly extracts conserved physical observables directly without multiplying capital or labor
 * by synthetic technology scalars ({@code AGENTS.md} §10.1, §10.4).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class StatVariableExtractor {

    private StatVariableExtractor() {
    }

    /**
     * Extracts a contiguous {@code float[]} slice for the requested observable key from
     * {@link WorldBuffer} (preferred) or {@link H3Cell} list.
     *
     * @param varName observable variable identifier
     * @param cells   fallback cell list
     * @param buffer  primary DOD world buffer
     * @return extracted observable array
     */
    public static float[] extractVariableArray(String varName, List<H3Cell> cells, WorldBuffer buffer) {
        String key = varName.toLowerCase(Locale.ROOT).trim();

        if (buffer != null) {
            int cap = buffer.getCapacity();
            switch (key) {
                case "wealth":
                case "gdp":
                case "capital":
                case "resourcecapital":
                    return buffer.getResourceCapital() != null ? buffer.getResourceCapital() : buffer.getBiomassHuman();
                case "population":
                case "pop":
                case "biomasshuman":
                    return buffer.getBiomassHuman();
                case "food":
                case "foodresource":
                    return buffer.getFoodResource() != null ? buffer.getFoodResource() : buffer.getBiomassNatural();
                case "water":
                case "rainfall":
                case "rain":
                    return buffer.getRainfall();
                case "temperature":
                case "temp":
                    return buffer.getTemperature();
                case "elevation":
                case "alt":
                    return buffer.getElevation();
                case "age":
                    return buffer.getBiomassHuman();
                case "tech":
                case "technology":
                case "technologylevel": {
                    float[] diag = new float[cap];
                    for (int i = 0; i < cap; i++) {
                        diag[i] = buffer.computeEmergentTechnologyDiagnostic(i);
                    }
                    return diag;
                }
                default:
                    break;
            }
        }

        if (cells != null && !cells.isEmpty()) {
            int size = cells.size();
            float[] res = new float[size];
            for (int i = 0; i < size; i++) {
                H3Cell c = cells.get(i);
                switch (key) {
                    case "wealth":
                    case "gdp":
                    case "capital":
                    case "resourcecapital":
                        res[i] = c.getResourceCapital() != null ? c.getResourceCapital().floatValue() : 0.0f;
                        break;
                    case "population":
                    case "pop":
                    case "biomasshuman":
                        res[i] = c.getPopulation() != null ? c.getPopulation().floatValue() : 0.0f;
                        break;
                    case "tech":
                    case "technology":
                    case "technologylevel":
                        res[i] = (float) org.ether.society.database.H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(c);
                        break;
                    case "lifespan":
                        res[i] = c.getLifespan() != null ? c.getLifespan().floatValue() : 40.0f;
                        break;
                    case "fertility":
                        res[i] = c.getFertility() != null ? c.getFertility().floatValue() : 6.0f;
                        break;
                    case "giniindex":
                    case "gini":
                        res[i] = c.getGiniIndex() != null ? c.getGiniIndex().floatValue() : 0.0f;
                        break;
                    case "food":
                    case "foodresource":
                        res[i] = c.getFoodResource() != null ? c.getFoodResource().floatValue() : 0.0f;
                        break;
                    case "water":
                    case "waterresource":
                        res[i] = c.getWaterResource() != null ? c.getWaterResource().floatValue() : 0.0f;
                        break;
                    case "aquifer":
                    case "freshwateraquifer":
                        res[i] = c.getFreshwaterAquifer() != null ? c.getFreshwaterAquifer().floatValue() : 0.0f;
                        break;
                    case "accessibleaquifer":
                        res[i] = c.getAccessibleAquifer() != null ? c.getAccessibleAquifer().floatValue() : 0.0f;
                        break;
                    case "wood":
                    case "woodresource":
                        res[i] = c.getWoodResource() != null ? c.getWoodResource().floatValue() : 0.0f;
                        break;
                    case "metal":
                    case "resourcemetal":
                        res[i] = c.getResourceMetal() != null ? c.getResourceMetal().floatValue() : 0.0f;
                        break;
                    case "preciousmetal":
                    case "resourcepreciousmetal":
                        res[i] = c.getResourcePreciousMetal() != null ? c.getResourcePreciousMetal().floatValue() : 0.0f;
                        break;
                    case "clay":
                    case "resourceclay":
                        res[i] = c.getResourceClay() != null ? c.getResourceClay().floatValue() : 0.0f;
                        break;
                    case "soilcarbon":
                    case "soilorganiccarbon":
                        res[i] = c.getSoilOrganicCarbon() != null ? c.getSoilOrganicCarbon().floatValue() : 0.0f;
                        break;
                    case "mantleheat":
                    case "mantleheatflow":
                        res[i] = c.getMantleHeatFlow() != null ? c.getMantleHeatFlow().floatValue() : 87.0f;
                        break;
                    case "temperature":
                    case "temp":
                        res[i] = c.getTemperature() != null ? c.getTemperature().floatValue() : 0.0f;
                        break;
                    case "rainfall":
                    case "rain":
                        res[i] = c.getRainfall() != null ? c.getRainfall().floatValue() : 0.0f;
                        break;
                    case "elevation":
                    case "alt":
                        res[i] = c.getElevation() != null ? c.getElevation().floatValue() : 0.0f;
                        break;
                    case "pollution":
                    case "pollutionlevel":
                        res[i] = c.getPollutionLevel() != null ? c.getPollutionLevel().floatValue() : 0.0f;
                        break;
                    case "albedo":
                    case "dynamicalbedo":
                        res[i] = c.getDynamicAlbedo() != null ? c.getDynamicAlbedo().floatValue() : 0.30f;
                        break;
                    case "movementfriction":
                    case "friction":
                        res[i] = c.getMovementFriction() != null ? c.getMovementFriction().floatValue() : 1.0f;
                        break;
                    case "biomassnatural":
                        res[i] = c.getBiomassNatural() != null ? c.getBiomassNatural().floatValue() : 0.0f;
                        break;
                    case "biomasslivestock":
                        res[i] = c.getBiomassLivestock() != null ? c.getBiomassLivestock().floatValue() : 0.0f;
                        break;
                    case "biomassfish":
                        res[i] = c.getBiomassFish() != null ? c.getBiomassFish().floatValue() : 0.0f;
                        break;
                    case "biomassagriculture":
                        res[i] = c.getBiomassAgriculture() != null ? c.getBiomassAgriculture().floatValue() : 0.0f;
                        break;
                    case "energywind":
                        res[i] = c.getEnergyWind() != null ? c.getEnergyWind().floatValue() : 0.0f;
                        break;
                    case "energysolar":
                        res[i] = c.getEnergySolar() != null ? c.getEnergySolar().floatValue() : 0.0f;
                        break;
                    case "energyfire":
                        res[i] = c.getEnergyFire() != null ? c.getEnergyFire().floatValue() : 0.0f;
                        break;
                    case "energyslaves":
                        res[i] = c.getEnergySlaves() != null ? c.getEnergySlaves().floatValue() : 0.0f;
                        break;
                    case "energyfoodconsumed":
                        res[i] = c.getEnergyFoodConsumed() != null ? c.getEnergyFoodConsumed().floatValue() : 0.0f;
                        break;
                    case "popyouth":
                        res[i] = c.getPopYouth() != null ? c.getPopYouth().floatValue() : 0.0f;
                        break;
                    case "popadult":
                        res[i] = c.getPopAdult() != null ? c.getPopAdult().floatValue() : 0.0f;
                        break;
                    case "popelderly":
                        res[i] = c.getPopElderly() != null ? c.getPopElderly().floatValue() : 0.0f;
                        break;
                    case "pop0to4":
                        res[i] = c.getPop0to4() != null ? c.getPop0to4().floatValue() : 0.0f;
                        break;
                    case "pop5to14":
                        res[i] = c.getPop5to14() != null ? c.getPop5to14().floatValue() : 0.0f;
                        break;
                    case "pop15to24":
                        res[i] = c.getPop15to24() != null ? c.getPop15to24().floatValue() : 0.0f;
                        break;
                    case "pop25to49":
                        res[i] = c.getPop25to49() != null ? c.getPop25to49().floatValue() : 0.0f;
                        break;
                    case "pop50to64":
                        res[i] = c.getPop50to64() != null ? c.getPop50to64().floatValue() : 0.0f;
                        break;
                    case "pop65to79":
                        res[i] = c.getPop65to79() != null ? c.getPop65to79().floatValue() : 0.0f;
                        break;
                    case "pop80plus":
                        res[i] = c.getPop80Plus() != null ? c.getPop80Plus().floatValue() : 0.0f;
                        break;
                    case "epidemicinfected":
                        res[i] = c.getEpidemicInfected() != null ? c.getEpidemicInfected().floatValue() : 0.0f;
                        break;
                    case "epidemicrecovered":
                        res[i] = c.getEpidemicRecovered() != null ? c.getEpidemicRecovered().floatValue() : 0.0f;
                        break;
                    case "food_per_capita": {
                        float p = c.getPopulation() != null ? c.getPopulation().floatValue() : 0.0f;
                        float f = c.getFoodResource() != null ? c.getFoodResource().floatValue() : 0.0f;
                        res[i] = p > 0 ? f / p : 0.0f;
                        break;
                    }
                    case "work":
                    case "resourcework": {
                        if (c.getResourceWork() != null && c.getResourceWork() > 0.0) {
                            res[i] = c.getResourceWork().floatValue();
                        } else {
                            float p = c.getPopulation() != null ? c.getPopulation().floatValue() : 0.0f;
                            float adult = c.getPopAdult() != null ? c.getPopAdult().floatValue() : p * 0.6f;
                            res[i] = adult;
                        }
                        break;
                    }
                    default:
                        res[i] = 0.0f;
                        break;
                }
            }
            return res;
        }

        return new float[0];
    }
}
