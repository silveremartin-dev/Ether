/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.database;

import org.ether.society.database.components.BiomassState;
import org.ether.society.database.components.ClimateState;
import org.ether.society.database.components.DemographicState;
import org.ether.society.database.components.MineralState;
import org.ether.society.model.Biome;

/**
 * <h1>H3CellPhysicsAndStateHelper</h1>
 * <p>
 * Stateless analytical physics, demography, surface geometry, snapshot cloning, and Data-Oriented Design (DOD)
 * component helper for {@link H3Cell}.<br>
 * Extracted from {@link H3Cell} to enforce the Single Responsibility Principle and keep the JPA entity concise.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see H3Cell
 */
public final class H3CellPhysicsAndStateHelper {

    private H3CellPhysicsAndStateHelper() {
        // Static helper utility class
    }

    /**
     * Calculates terrain movement friction (cost-distance multiplier) based on elevation,
     * biome, and technology level:
     * $$f_{\text{move}} = \max\left(0.3, f_{\text{base}}(\text{elev}, \text{biome}) \cdot \max(0.2, 1.0 - 0.08 \cdot T)\right)$$
     *
     * @param cell      target H3 cell
     * @param techLevel technology level ($1.0$ = ancient, $10.0$ = modern)
     * @return movement friction multiplier ($1.0$ = flat plain, $>10.0$ = extreme mountain/desert/swamp)
     */
    public static double calculateMovementFriction(H3Cell cell, double techLevel) {
        double baseFriction = 1.0;

        double elev = cell.getElevationOr(0.0);
        if (elev > 3000) baseFriction += 8.0;
        else if (elev > 1500) baseFriction += 4.0;
        else if (elev > 500) baseFriction += 1.5;
        else if (elev < 0) {
            return techLevel >= 3.0 ? Math.max(0.5, 3.0 - (techLevel * 0.25)) : 25.0;
        }

        Biome biome = cell.getBiome();
        if (biome != null) {
            switch (biome) {
                case TUNDRA, SNOW -> baseFriction += 3.5;
                case GLACIER -> baseFriction += 8.0;
                case FOREST -> baseFriction += 1.5;
                case SAVANNAH -> baseFriction += 0.8;
                case JUNGLE -> baseFriction += 4.5;
                case DESERT -> baseFriction += 4.0;
                case MOUNTAINS -> baseFriction += 6.0;
                case HILLS -> baseFriction += 2.0;
                case BEACH -> baseFriction += 1.2;
                default -> {}
            }
        }

        double techMitigation = Math.max(0.2, 1.0 - (techLevel * 0.08));
        double computed = Math.max(0.3, baseFriction * techMitigation);
        cell.setMovementFriction(computed);
        return computed;
    }

    /**
     * Calculates dynamic surface albedo $\alpha \in [0.06, 0.85]$ based on snow/ice cover, biome,
     * and natural vegetation density:
     * $$\alpha = \alpha_{\text{biome}} \cdot v_{\text{veg}} + 0.30 \cdot (1 - v_{\text{veg}})$$
     *
     * @param cell target H3 cell
     * @return updated surface albedo $\alpha$
     */
    public static double calculateDynamicAlbedo(H3Cell cell) {
        if (cell.getElevation() != null && cell.getElevation() < 0) {
            cell.setDynamicAlbedo(0.06);
            return 0.06;
        }

        double baseAlbedo = 0.22;
        Biome biome = cell.getBiome();
        if (biome != null) {
            switch (biome) {
                case SNOW, TUNDRA -> baseAlbedo = 0.78;
                case GLACIER -> baseAlbedo = 0.85;
                case SAVANNAH -> baseAlbedo = 0.25;
                case DESERT -> baseAlbedo = 0.40;
                case FOREST, JUNGLE -> baseAlbedo = 0.12;
                case MOUNTAINS -> baseAlbedo = 0.35;
                default -> baseAlbedo = 0.22;
            }
        }

        double vegDensity = Math.clamp(cell.getBiomassNaturalOr(0.0) / 1000.0, 0.0, 1.0);
        double computed = (baseAlbedo * vegDensity) + (0.30 * (1.0 - vegDensity));
        cell.setDynamicAlbedo(computed);
        return computed;
    }

    /**
     * Updates the detailed 7-segment age pyramid based on total population and demographic transition stage.
     *
     * @param cell      target H3 cell
     * @param techLevel technological level driving demographic transition interpolation
     */
    public static void updateAgePyramidFromTotal(H3Cell cell, double techLevel) {
        Integer population = cell.getPopulation();
        if (population == null || population <= 0) {
            cell.setPop0to4(0);
            cell.setPop5to14(0);
            cell.setPop15to24(0);
            cell.setPop25to49(0);
            cell.setPop50to64(0);
            cell.setPop65to79(0);
            cell.setPop80Plus(0);
            cell.setPopYouth(0);
            cell.setPopAdult(0);
            cell.setPopElderly(0);
            return;
        }

        double tf = Math.clamp((techLevel - 1.0) / 7.0, 0.0, 1.0);

        double share0to4   = 0.15 - (tf * 0.10);
        double share5to14  = 0.27 - (tf * 0.14);
        double share15to24 = 0.18 - (tf * 0.04);
        double share25to49 = 0.25 + (tf * 0.09);
        double share50to64 = 0.09 + (tf * 0.07);
        double share65to79 = 0.05 + (tf * 0.09);

        int p0to4   = (int) Math.round(population * share0to4);
        int p5to14  = (int) Math.round(population * share5to14);
        int p15to24 = (int) Math.round(population * share15to24);
        int p25to49 = (int) Math.round(population * share25to49);
        int p50to64 = (int) Math.round(population * share50to64);
        int p65to79 = (int) Math.round(population * share65to79);

        int sum6 = p0to4 + p5to14 + p15to24 + p25to49 + p50to64 + p65to79;
        int p80Plus = Math.max(0, population - sum6);

        cell.setPop0to4(p0to4);
        cell.setPop5to14(p5to14);
        cell.setPop15to24(p15to24);
        cell.setPop25to49(p25to49);
        cell.setPop50to64(p50to64);
        cell.setPop65to79(p65to79);
        cell.setPop80Plus(p80Plus);

        cell.setPopYouth(p0to4 + p5to14);
        cell.setPopAdult(p15to24 + p25to49 + p50to64);
        cell.setPopElderly(p65to79 + p80Plus);
    }

    /**
     * Calculates the total annual metabolic subsistence energy required by the cell's population in $\text{GJ/year}$:
     * $$E_{\text{metabolic}} = \left(\sum_{c=0}^{6} N_c \cdot e_c\right) \cdot \mu_{\text{PAL}}(K/N, T) \cdot \mu_{\text{species}}$$
     *
     * @param cell target H3 cell
     * @return annual caloric requirement in Gigajoules ($\text{GJ}$)
     */
    public static double calculateAnnualMetabolicNeedGJ(H3Cell cell) {
        int totalPop = cell.getPopulationOr(0);
        if (totalPop <= 0) return 0.0;

        int[] cohorts = cell.getAgePyramid();
        double cohortMetabolicSum = (cohorts[0] * 1.60)
                                  + (cohorts[1] * 2.50)
                                  + (cohorts[2] * 3.80)
                                  + (cohorts[3] * 3.50)
                                  + (cohorts[4] * 3.20)
                                  + (cohorts[5] * 2.70)
                                  + (cohorts[6] * 2.20);

        double basePerCapitaGJ = cohortMetabolicSum / totalPop;

        double capPerCapita = cell.getResourceCapitalOr(0.0) / totalPop;
        double tech = cell.getTechnologyLevelOr(1.0);
        double mechanizationFactor = Math.clamp(capPerCapita / 40.0 + (tech >= 3.0 ? (tech - 3.0) * 0.2 : 0.0), 0.0, 1.0);
        double laborMultiplier = 1.25 - (0.30 * mechanizationFactor);

        if (cell.getResourceMetalOr(0.0) > 10.0 || cell.getResourceCoalOr(0.0) > 50.0) {
            laborMultiplier += 0.08;
        }

        double speciesMultiplier = switch (cell.getHomininSpecies()) {
            case 2 -> 1.40; // Homo Neanderthalensis
            case 3 -> 1.20; // Denisovan / Archaic
            default -> 1.0; // Homo Sapiens
        };

        return totalPop * basePerCapitaGJ * laborMultiplier * speciesMultiplier;
    }

    /**
     * Standard average H3 hexagon area in $\text{km}^2$ according to resolution level ($0$ to $15$).
     *
     * @param resolution H3 resolution level ($0\dots 15$)
     * @return average hexagon area in $\text{km}^2$
     */
    public static double getAvgHexagonAreaKm2(int resolution) {
        return switch (resolution) {
            case 0 -> 4357449.42;
            case 1 -> 609788.44;
            case 2 -> 86801.78;
            case 3 -> 12393.43;
            case 4 -> 1770.49;
            case 5 -> 252.93;
            case 6 -> 36.1329;
            case 7 -> 5.1618;
            case 8 -> 0.7373276;
            case 9 -> 0.1053325;
            case 10 -> 0.0150475;
            default -> (resolution < 0) ? 4357449.42 : (0.7373276 * Math.pow(7.0, 8 - resolution));
        };
    }

    /**
     * Calculates the effective 3D real surface area of the cell in $\text{km}^2$, accounting for
     * embedded H3 resolution and topographical slope expansion:
     * $$A_{3\text{D}} = A_{\text{H3}}(r) \cdot \sqrt{1 + 0.15 \cdot \max(0, f_{\text{move}} - 1)^{1.8}}$$
     *
     * @param cell target H3 cell
     * @return effective 3D surface area in $\text{km}^2$
     */
    public static double getEffectiveSurfaceAreaKm2(H3Cell cell) {
        int res = 3;
        Long h3Index = cell.getH3Index();
        if (h3Index != null && h3Index != 0) {
            int r = (int) ((h3Index >>> 52) & 0xF);
            if (r >= 0 && r <= 15) {
                res = r;
            }
        }
        double baseArea = getAvgHexagonAreaKm2(res);
        double friction = cell.getMovementFrictionOr(1.0);
        double slopeFactor3D = Math.sqrt(1.0 + 0.15 * Math.pow(Math.max(0.0, friction - 1.0), 1.8));
        return baseArea * slopeFactor3D;
    }

    /**
     * Creates a deep snapshot copy of the given {@link H3Cell}.
     *
     * @param src source H3 cell
     * @return cloned snapshot instance
     */
    public static H3Cell snapshot(H3Cell src) {
        H3Cell copy = new H3Cell(src.getH3Index(), src.getLatitude(), src.getLongitude());
        copy.setId(src.getId());
        copy.setElevation(src.getElevation());
        copy.setTemperature(src.getTemperature());
        copy.setRainfall(src.getRainfall());
        copy.setBiome(src.getBiome());
        copy.setPopulation(src.getPopulation());
        copy.setFoodResource(src.getFoodResource());
        copy.setWaterResource(src.getWaterResource());
        copy.setWoodResource(src.getWoodResource());
        copy.setResourceMetal(src.getResourceMetal());
        copy.setResourcePreciousMetal(src.getResourcePreciousMetal());
        copy.setSoilOrganicCarbon(src.getSoilOrganicCarbon());
        copy.setSoilSalinity(src.getSoilSalinity());
        copy.setMantleHeatFlow(src.getMantleHeatFlow());
        copy.setFreshwaterAquifer(src.getFreshwaterAquifer());
        copy.setAccessibleAquifer(src.getAccessibleAquifer());
        copy.setPollutionLevel(src.getPollutionLevel());
        copy.setResourceClay(src.getResourceClay());
        copy.setResourceWork(src.getResourceWork());
        copy.setResourceCapital(src.getResourceCapital());
        copy.setLifespan(src.getLifespan());
        copy.setFertility(src.getFertility());
        copy.setGiniIndex(src.getGiniIndex());
        copy.setTechnologyLevel(src.getTechnologyLevel());
        copy.setHomininSpecies(src.getHomininSpecies());
        copy.setOwner(src.getOwner());
        copy.setFluxPressure(src.getFluxPressure());
        copy.setBoundaryCell(src.isBoundaryCell());
        copy.setMovementFriction(src.getMovementFriction());
        copy.setChokePointScore(src.getChokePointScore());
        copy.setTransitTollRevenue(src.getTransitTollRevenue());
        copy.setPopYouth(src.getPopYouth());
        copy.setPopAdult(src.getPopAdult());
        copy.setPopElderly(src.getPopElderly());
        copy.setIsCoastal(src.getIsCoastal());
        copy.setCoastalMarineResource(src.getCoastalMarineResource());
        copy.setIsPolder(src.getIsPolder());
        copy.setHasFloatingInfrastructure(src.getHasFloatingInfrastructure());

        copy.setBiomassHuman(src.getBiomassHuman());
        copy.setBiomassLivestock(src.getBiomassLivestock());
        copy.setBiomassFish(src.getBiomassFish());
        copy.setBiomassAgriculture(src.getBiomassAgriculture());
        copy.setBiomassNatural(src.getBiomassNatural());
        copy.setEnergyWind(src.getEnergyWind());
        copy.setEnergySolar(src.getEnergySolar());
        copy.setEnergyFire(src.getEnergyFire());
        copy.setEnergySlaves(src.getEnergySlaves());
        copy.setEnergyFoodConsumed(src.getEnergyFoodConsumed());

        copy.setHabitatType(src.getHabitatType());
        copy.setHabitatTier(src.getHabitatTier());
        copy.setHabitatCapacity(src.getHabitatCapacity());
        copy.setHabitatIntegrity(src.getHabitatIntegrity());
        copy.setHabitatEnergyKw(src.getHabitatEnergyKw());
        copy.setHabitatAgeYears(src.getHabitatAgeYears());

        return copy;
    }

    /**
     * Extracts an immutable {@link ClimateState} component from the cell.
     *
     * @param cell source H3 cell
     * @return current climate state component
     */
    public static ClimateState extractClimateState(H3Cell cell) {
        return new ClimateState(
                cell.getTemperatureOr(15.0),
                cell.getRainfallOr(800.0),
                cell.getDynamicAlbedoOr(0.30),
                cell.getIceSheetThicknessMetersOr(0.0),
                cell.getSeaLevelOffsetMetersOr(0.0)
        );
    }

    /**
     * Applies an immutable {@link ClimateState} component to the cell.
     *
     * @param cell  target H3 cell
     * @param state climate state component to apply
     */
    public static void applyClimateState(H3Cell cell, ClimateState state) {
        if (state == null) return;
        cell.setTemperature(state.temperature());
        cell.setRainfall(state.rainfall());
        cell.setDynamicAlbedo(state.dynamicAlbedo());
        cell.setIceSheetThicknessMeters(state.iceSheetThicknessMeters());
        cell.setSeaLevelOffsetMeters(state.seaLevelOffsetMeters());
    }

    /**
     * Extracts an immutable {@link BiomassState} component from the cell.
     *
     * @param cell source H3 cell
     * @return current biomass state component
     */
    public static BiomassState extractBiomassState(H3Cell cell) {
        return new BiomassState(
                cell.getBiomassHumanOr(0.0),
                cell.getBiomassLivestockOr(0.0),
                cell.getBiomassFishOr(0.0),
                cell.getBiomassAgricultureOr(0.0),
                cell.getBiomassNaturalOr(100.0),
                cell.getFoodResourceOr(50.0),
                cell.getWaterResourceOr(50.0),
                cell.getWoodResourceOr(50.0),
                cell.getSoilOrganicCarbonOr(10.0),
                cell.getSoilSalinityOr(0.0)
        );
    }

    /**
     * Applies an immutable {@link BiomassState} component to the cell.
     *
     * @param cell  target H3 cell
     * @param state biomass state component to apply
     */
    public static void applyBiomassState(H3Cell cell, BiomassState state) {
        if (state == null) return;
        cell.setBiomassHuman(state.biomassHuman());
        cell.setBiomassLivestock(state.biomassLivestock());
        cell.setBiomassFish(state.biomassFish());
        cell.setBiomassAgriculture(state.biomassAgriculture());
        cell.setBiomassNatural(state.biomassNatural());
        cell.setFoodResource(state.foodResource());
        cell.setWaterResource(state.waterResource());
        cell.setWoodResource(state.woodResource());
        cell.setSoilOrganicCarbon(state.soilOrganicCarbon());
        cell.setSoilSalinity(state.soilSalinity());
    }

    /**
     * Extracts an immutable {@link DemographicState} component from the cell.
     *
     * @param cell source H3 cell
     * @return current demographic state component
     */
    public static DemographicState extractDemographicState(H3Cell cell) {
        return new DemographicState(
                cell.getPopulationOr(0),
                cell.getLifespanOr(40.0),
                cell.getFertilityOr(6.0),
                cell.getGiniIndexOr(0.0),
                cell.getTechnologyLevelOr(0.0),
                cell.getHomininSpeciesOr(1)
        );
    }

    /**
     * Applies an immutable {@link DemographicState} component to the cell.
     *
     * @param cell  target H3 cell
     * @param state demographic state component to apply
     */
    public static void applyDemographicState(H3Cell cell, DemographicState state) {
        if (state == null) return;
        cell.setPopulation(state.population());
        cell.setLifespan(state.lifespan());
        cell.setFertility(state.fertility());
        cell.setGiniIndex(state.giniIndex());
        cell.setTechnologyLevel(state.technologyLevel());
        cell.setHomininSpecies(state.homininSpecies());
    }

    /**
     * Extracts an immutable {@link MineralState} component from the cell.
     *
     * @param cell source H3 cell
     * @return current mineral state component
     */
    public static MineralState extractMineralState(H3Cell cell) {
        return new MineralState(
                cell.getResourceCoalOr(0.0),
                cell.getResourceOilOr(0.0),
                cell.getResourceGasOr(0.0),
                cell.getResourceUraniumOr(0.0),
                cell.getResourceMetalOr(0.0),
                cell.getResourcePreciousMetalOr(0.0),
                cell.getResourceRareEarthsOr(0.0),
                cell.getMantleHeatFlowOr(87.0),
                cell.getFreshwaterAquiferOr(0.0),
                cell.getAccessibleAquiferOr(0.0)
        );
    }

    /**
     * Applies an immutable {@link MineralState} component to the cell.
     *
     * @param cell  target H3 cell
     * @param state mineral state component to apply
     */
    public static void applyMineralState(H3Cell cell, MineralState state) {
        if (state == null) return;
        cell.setResourceCoal(state.resourceCoal());
        cell.setResourceOil(state.resourceOil());
        cell.setResourceGas(state.resourceGas());
        cell.setResourceUranium(state.resourceUranium());
        cell.setResourceMetal(state.resourceMetal());
        cell.setResourcePreciousMetal(state.resourcePreciousMetal());
        cell.setResourceRareEarths(state.resourceRareEarths());
        cell.setMantleHeatFlow(state.mantleHeatFlow());
        cell.setFreshwaterAquifer(state.freshwaterAquifer());
        cell.setAccessibleAquifer(state.accessibleAquifer());
    }
}
