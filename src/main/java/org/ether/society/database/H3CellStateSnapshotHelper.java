/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.database;

import org.ether.society.database.components.BiomassState;
import org.ether.society.database.components.ClimateState;
import org.ether.society.database.components.DemographicState;
import org.ether.society.database.components.MineralState;

/**
 * Stateless deep-snapshot cloner and Data-Oriented Design (DOD) component state
 * projector for {@link H3Cell}.
 *
 * <p>Strictly projects conserved biophysical, geological, and demographic observables
 * without synthetic state-level technology fields ({@code AGENTS.md} Section 10.1).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 * @see H3Cell
 * @see H3CellPhysicsAndStateHelper
 */
public final class H3CellStateSnapshotHelper {

    private H3CellStateSnapshotHelper() {
        // Static utility class
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
        copy.setResourceCoal(src.getResourceCoal());
        copy.setResourceOil(src.getResourceOil());
        copy.setResourceGas(src.getResourceGas());
        copy.setResourceUranium(src.getResourceUranium());
        copy.setResourceRareEarths(src.getResourceRareEarths());
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
        copy.setHasDam(src.getHasDam());
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
