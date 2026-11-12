/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.core.dod;

import org.ether.society.core.math.SmoothMath;
import org.ether.society.database.H3CellPhysicsAndStateHelper;
import org.ether.society.database.components.BiomassState;
import org.ether.society.database.components.ClimateState;
import org.ether.society.database.components.DemographicState;
import org.ether.society.database.components.MineralState;
import org.ether.society.model.OrganismBiophysicalProfile;

/**
 * Adapter and diagnostic utility for {@link WorldBuffer}.
 * Bridges Data-Oriented Design (SoA primitive arrays) with object component models
 * and computes thermodynamic power flux and emergent technology diagnostics.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class WorldBufferComponentAdapter {

    private WorldBufferComponentAdapter() {
        // Utility class
    }

    /**
     * Extracts an immutable {@link ClimateState} component for a cell index.
     *
     * @param buffer source world buffer
     * @param index cell buffer index
     * @return climate state component
     */
    public static ClimateState getClimateState(WorldBuffer buffer, int index) {
        return new ClimateState(
                buffer.getTemperature()[index],
                buffer.getRainfall()[index],
                0.30,
                0.0,
                0.0
        );
    }

    /**
     * Updates buffer climate variables from an immutable {@link ClimateState} component.
     *
     * @param buffer target world buffer
     * @param index cell buffer index
     * @param state climate state component
     */
    public static void setClimateState(WorldBuffer buffer, int index, ClimateState state) {
        if (state == null) return;
        buffer.getTemperature()[index] = (float) state.temperature();
        buffer.getRainfall()[index] = (float) state.rainfall();
    }

    /**
     * Extracts an immutable {@link BiomassState} component for a cell index.
     *
     * @param buffer source world buffer
     * @param index cell buffer index
     * @return biomass state component
     */
    public static BiomassState getBiomassState(WorldBuffer buffer, int index) {
        return new BiomassState(
                buffer.getBiomassHuman()[index],
                buffer.getBiomassLivestock()[index],
                buffer.getBiomassFish()[index],
                buffer.getBiomassAgriculture()[index],
                buffer.getBiomassNatural()[index],
                buffer.getFoodResource()[index],
                buffer.getWaterResource()[index],
                buffer.getWoodResource()[index],
                10.0,
                0.0
        );
    }

    /**
     * Updates buffer biomass variables from an immutable {@link BiomassState} component.
     *
     * @param buffer target world buffer
     * @param index cell buffer index
     * @param state biomass state component
     */
    public static void setBiomassState(WorldBuffer buffer, int index, BiomassState state) {
        if (state == null) return;
        buffer.getBiomassHuman()[index] = (float) state.biomassHuman();
        buffer.getBiomassLivestock()[index] = (float) state.biomassLivestock();
        buffer.getBiomassFish()[index] = (float) state.biomassFish();
        buffer.getBiomassAgriculture()[index] = (float) state.biomassAgriculture();
        buffer.getBiomassNatural()[index] = (float) state.biomassNatural();
        buffer.getFoodResource()[index] = (float) state.foodResource();
        buffer.getWaterResource()[index] = (float) state.waterResource();
        buffer.getWoodResource()[index] = (float) state.woodResource();
    }

    /**
     * Extracts an immutable {@link DemographicState} component for a cell index.
     *
     * @param buffer source world buffer
     * @param index cell buffer index
     * @return demographic state component
     */
    public static DemographicState getDemographicState(WorldBuffer buffer, int index) {
        return new DemographicState(
                (int) buffer.getBiomassHuman()[index],
                buffer.getLifespan()[index],
                buffer.getFertility()[index],
                buffer.getGiniIndex()[index],
                1
        );
    }

    /**
     * Updates buffer demographic variables from an immutable {@link DemographicState} component.
     *
     * @param buffer target world buffer
     * @param index cell buffer index
     * @param state demographic state component
     */
    public static void setDemographicState(WorldBuffer buffer, int index, DemographicState state) {
        if (state == null) return;
        buffer.getLifespan()[index] = (float) state.lifespan();
        buffer.getFertility()[index] = (float) state.fertility();
        buffer.getGiniIndex()[index] = (float) state.giniIndex();
    }

    /**
     * Extracts an immutable {@link MineralState} component for a cell index.
     *
     * @param buffer source world buffer
     * @param index cell buffer index
     * @return mineral state component
     */
    public static MineralState getMineralState(WorldBuffer buffer, int index) {
        return new MineralState(
                buffer.getResourceCoal()[index],
                buffer.getResourceOil()[index],
                buffer.getResourceGas()[index],
                buffer.getResourceUranium()[index],
                buffer.getMetalResource()[index],
                0.0,
                0.0,
                87.0,
                buffer.getFreshwaterAquifer()[index],
                buffer.getFreshwaterAquifer()[index]
        );
    }

    /**
     * Updates buffer mineral variables from an immutable {@link MineralState} component.
     *
     * @param buffer target world buffer
     * @param index cell buffer index
     * @param state mineral state component
     */
    public static void setMineralState(WorldBuffer buffer, int index, MineralState state) {
        if (state == null) return;
        buffer.getResourceCoal()[index] = (float) state.resourceCoal();
        buffer.getResourceOil()[index] = (float) state.resourceOil();
        buffer.getResourceGas()[index] = (float) state.resourceGas();
        buffer.getResourceUranium()[index] = (float) state.resourceUranium();
        buffer.getMetalResource()[index] = (float) state.resourceMetal();
        buffer.getFreshwaterAquifer()[index] = (float) state.freshwaterAquifer();
    }

    /**
     * Calculates total somatic and exosomatic mechanical and thermal power flux per individual in Watts,
     * calibrated to Vaclav Smil (2017) <i>Energy and Civilization: A History</i>.
     *
     * @param buffer source world buffer
     * @param index cell buffer index
     * @return total continuous power dissipated per capita in Watts (\(\text{W}\))
     */
    public static float computeCellPowerWattsPerCapita(WorldBuffer buffer, int index) {
        if (index < 0 || index >= buffer.getCapacity()) return 0.0f;
        float pop = buffer.getBiomassHuman()[index];
        if (pop <= 0.001f) return 0.0f;
        OrganismBiophysicalProfile organismProfile = buffer.getOrganismProfile();
        double basalW = organismProfile.basalMetabolicPowerWatts();
        double kpc = Math.max(0.0, buffer.getResourceCapital()[index] / pop);
        // 1. Somatic basal + mechanical labor + domestic hearth/firewood thermal baseline (~348 W/cap in foraging bands, Smil 2017)
        double woodThermalW = 212.0 * Math.tanh((buffer.getBiomassNatural()[index] + buffer.getWoodResource()[index] + 50.0) / 50.0);
        // 2. Agrarian draft traction & plows (activates smoothly above kpc ~ 150 kg/cap, up to +25 W/cap global average)
        double draftW = 25.0 * Math.tanh(SmoothMath.softplus(kpc - 150.0, 0.05) / 250.0);
        // 3. Waterwheels, windmills, sail & proto-industrial metallurgical furnaces (activates smoothly above kpc ~ 520 kg/cap, up to +220 W/cap)
        double millAndCharcoalW = 220.0 * Math.tanh(SmoothMath.softplus(kpc - 520.0, 0.05) / 420.0);
        // 4. Coal/steam & early internal combustion prime movers (activates smoothly above kpc ~ 1000 kg/cap, up to +600 W/cap)
        double steamCoalW = 600.0 * Math.tanh(SmoothMath.softplus(kpc - 1000.0, 0.02) / 1800.0);
        // 5. Modern electric/hydrocarbon/nuclear/solar grid (activates smoothly above kpc ~ 4000 kg/cap, up to +1350 W/cap)
        double modernGridW = 1350.0 * Math.tanh(SmoothMath.softplus(kpc - 4000.0, 0.01) / 10000.0);
        return (float) (basalW + 39.0 + woodThermalW + draftW + millAndCharcoalW + steamCoalW + modernGridW);
    }

    /**
     * Computes the strictly <i>a posteriori</i> Garrett-Lotka emergent exergy-capital diagnostic index
     * for a cell buffer index.
     *
     * @param buffer source world buffer
     * @param index primitive cell buffer index
     * @return dimensionless emergent technology diagnostic in \([0.0, 10.0]\)
     */
    public static float computeEmergentTechnologyDiagnostic(WorldBuffer buffer, int index) {
        if (index < 0 || index >= buffer.getCapacity()) return 0.0f;
        float pop = buffer.getBiomassHuman()[index];
        if (pop > 0.001f) {
            double powerW = computeCellPowerWattsPerCapita(buffer, index);
            double kpc = Math.max(0.0, buffer.getResourceCapital()[index] / pop);
            return (float) H3CellPhysicsAndStateHelper.computeGarrettLotkaEmergentIndex(
                    powerW, kpc, buffer.getOrganismProfile().basalMetabolicPowerWatts());
        }
        return 0.0f;
    }
}
