/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.database;

import org.ether.society.core.math.SmoothMath;
import org.ether.society.database.components.BiomassState;
import org.ether.society.database.components.ClimateState;
import org.ether.society.database.components.DemographicState;
import org.ether.society.database.components.MineralState;
import org.ether.society.engines.tier1.PhysicalEnergyGridEngine;
import org.ether.society.model.OrganismBiophysicalProfile;

/**
 * Stateless analytical physics, Lotka stable demography, surface geometry, Garrett-Lotka
 * <i>a posteriori</i> diagnostic evaluation, and Data-Oriented Design (DOD) component helper for {@link H3Cell}.
 *
 * <p>Strictly enforces {@code AGENTS.md} Section 10: all equations are \(C^\infty\) smooth functions
 * of conserved primary biophysical observables with zero state-level technology dependence.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see H3Cell
 * @see H3CellStateSnapshotHelper
 */
public final class H3CellPhysicsAndStateHelper {

    /**
     * Garrett (2011, <i>Climatic Change</i> 104:437) thermodynamic maintenance dissipation constant
     * \(\lambda_{\text{maint}} = 8.0\,\text{W}\) per physical capital unit, linking active wealth stock
     * to continuous exosomatic power dissipation.
     */
    public static final double GARRETT_CAPITAL_MAINTENANCE_WATTS_PER_UNIT = 8.0;

    /** Reference somatic tool capital normalization \(k_0 = 5.0\) physical units/individual. */
    public static final double REFERENCE_SOMATIC_CAPITAL_PER_CAPITA = 5.0;

    private H3CellPhysicsAndStateHelper() {
        // Static helper utility class
    }

    /**
     * Evaluates the canonical \(C^\infty\) Garrett-Lotka emergent exergy-capital diagnostic \(T_{\text{emergent}}\)
     * strictly <i>a posteriori</i> from conserved power flux and physical capital stock:
     *
     * <p>$$\mathcal{M} = \frac{p_{\text{eff}}}{B_{\text{basal}}}, \qquad
     * k_{\text{energized}} = \frac{\operatorname{softplus}(p_{\text{eff}}; 0.8 B_{\text{basal}}, 0.2 B_{\text{basal}})}{\lambda_{\text{maint}}}, \qquad
     * k_{\text{active}} = \frac{2 \cdot k \cdot k_{\text{energized}}}{k + k_{\text{energized}} + \varepsilon}$$</p>
     * <p>$$T_{\text{emergent}}(\mathcal{M}, k_{\text{active}}) = 0.50 + 1.45 \log_{10}(\mathcal{M}) + 0.58 \log_{10}\!\left(1 + \frac{k_{\text{active}}}{k_0}\right)$$</p>
     *
     * <p>Resolves the post-collapse depopulation paradox (\(N \to 0 \implies K/N \to \infty\)): if power plants
     * cease operating (\(p \to B_{\text{basal}}\)), \(k_{\text{energized}}\) remains bounded at somatic maintenance
     * capacity, preventing unphysical blow-up of \(k_{\text{active}}\).</p>
     *
     * @param totalPowerWattsPerIndividual somatic + exosomatic power captured per individual (\(\text{W}\))
     * @param grossCapitalPerIndividual    gross structural capital stock per individual (\(K/N\))
     * @param basalMetabolicWatts          Kleiber basal metabolic rate \(B_{\text{basal}}(M_b)\) (\(\text{W}\))
     * @return dimensionless emergent telemetry diagnostic bounded smoothly in \([0.0, 10.0]\)
     */
    public static double computeGarrettLotkaEmergentIndex(
            double totalPowerWattsPerIndividual,
            double grossCapitalPerIndividual,
            double basalMetabolicWatts) {
        double basal = Math.max(1.0e-9, basalMetabolicWatts);
        double pEff = SmoothMath.softmax(basal, totalPowerWattsPerIndividual, 0.15 * basal);
        double metabolicRatio = pEff / basal;

        double kGross = SmoothMath.softplus(grossCapitalPerIndividual, 0.0, 20.0);
        double kEnergized = SmoothMath.softplus(pEff - 0.80 * basal, 20.0)
                / GARRETT_CAPITAL_MAINTENANCE_WATTS_PER_UNIT;
        double kActive = SmoothMath.harmonicCoupling(kGross, kEnergized);

        double raw = 0.50
                + 1.45 * Math.log10(Math.max(1.0, metabolicRatio))
                + 0.58 * Math.log10(1.0 + kActive / REFERENCE_SOMATIC_CAPITAL_PER_CAPITA);
        return SmoothMath.smoothClamp(raw, 0.0, 10.0, 20.0);
    }

    /**
     * Computes the strictly <i>a posteriori</i> Garrett-Lotka emergent telemetry diagnostic for a cell.
     * Returns {@code 0.0} for uninhabited cells (\(N \le 0\)).
     *
     * @param cell target H3 cell
     * @return read-only emergent diagnostic in \([0.0, 10.0]\)
     */
    public static double computeEmergentTechnologyDiagnostic(H3Cell cell) {
        if (cell == null) return 0.0;
        int pop = cell.getPopulationOr(0);
        if (pop <= 0) return 0.0;

        double basalWatts = OrganismBiophysicalProfile.HOMO_SAPIENS.basalMetabolicPowerWatts();
        double exosomaticWatts = PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(cell)
                + (cell.getHabitatEnergyKwOr(0.0) * 1000.0) / pop;
        double totalPowerWatts = basalWatts + exosomaticWatts;
        double grossKpc = cell.getResourceCapitalOr(0.0) / pop;

        return computeGarrettLotkaEmergentIndex(totalPowerWatts, grossKpc, basalWatts);
    }

    /**
     * Calculates \(C^\infty\) continuous terrain movement friction (cost-distance multiplier) based on
     * topographic relief (Minetti et al. 2002), standing vegetation impedance, cryospheric snow
     * resistance, and physical transport infrastructure capital density:
     * $$f_{\text{move}} = \operatorname{sclamp}\!\left((f_{\text{relief}} + f_{\text{veg}} + f_{\text{snow}}) \cdot f_{\text{infra}},\; 0.30,\; 25.0\right)$$
     *
     * @param cell target H3 cell
     * @return updated movement friction multiplier (\(1.0\) = flat plain, \(&gt;10.0\) = rugged/glaciated)
     */
    public static double calculateMovementFriction(H3Cell cell) {
        if (cell == null) return 1.0;
        double elev = cell.getElevationOr(0.0);
        double capital = cell.getResourceCapitalOr(0.0);
        double tempC = cell.getTemperatureOr(15.0);

        // Smooth bathymetric transition across coastline (elev = 0 m)
        double landWeight = SmoothMath.sigmoid(elev, 0.0, 5.0);

        // Marine navigation friction mitigated by port/naval physical capital and sea-ice resistance
        double seaIcePenalty = 12.0 * SmoothMath.sigmoid(-tempC, 1.5, 1.0);
        double seaFriction = SmoothMath.smoothClamp(
                (25.0 + seaIcePenalty) / (1.0 + 0.22 * Math.log1p(capital / 80.0)),
                0.40, 25.0, 10.0);

        // Terrestrial Minetti incline cost + canopy impedance + cryospheric snow resistance
        double posElev = SmoothMath.softplus(elev, 0.0, 15.0);
        double fRelief = 1.0 + 4.0 * Math.pow(posElev / 2500.0, 1.35);
        double biomassNat = SmoothMath.softplus(cell.getBiomassNaturalOr(0.0), 0.0, 10.0);
        double fVeg = 2.5 * Math.tanh(biomassNat / 3500.0);
        double fSnow = 4.5 * SmoothMath.sigmoid(-tempC, 0.0, 2.0);
        double baseLandFriction = fRelief + fVeg + fSnow;

        double areaKm2 = Math.max(10.0, getEffectiveSurfaceAreaKm2(cell));
        double capDensity = capital / areaKm2;
        double infraMitigation = 0.15 + 0.85 / (1.0 + 0.25 * Math.log1p(SmoothMath.softplus(capDensity * 10.0, 0.0, 0.5)));
        double landFriction = SmoothMath.smoothClamp(baseLandFriction * infraMitigation, 0.30, 25.0, 10.0);

        double computed = landWeight * landFriction + (1.0 - landWeight) * seaFriction;
        cell.setMovementFriction(computed);
        return computed;
    }

    /**
     * Calculates terrain movement friction with legacy technology level argument.
     *
     * @param cell target H3 cell
     * @param techLevel technology level
     * @return movement friction multiplier
     */
    public static double calculateMovementFriction(H3Cell cell, double techLevel) {
        return calculateMovementFriction(cell);
    }

    /**
     * Calculates \(C^\infty\) dynamic surface albedo \(\alpha \in [0.06, 0.85]\) using the
     * Sellers (1969) / Dickinson et al. (1993, BATS) thermodynamic surface energy balance model:
     * $$\alpha = (1 - f_{\text{snow}}) [f_{\text{veg}} \alpha_{\text{veg}} + (1 - f_{\text{veg}}) \alpha_{\text{soil}}] + f_{\text{snow}} \alpha_{\text{snow}}$$
     *
     * @param cell target H3 cell
     * @return updated surface albedo \(\alpha\)
     */
    public static double calculateDynamicAlbedo(H3Cell cell) {
        if (cell == null) return 0.22;
        double elev = cell.getElevationOr(0.0);
        double tempC = cell.getTemperatureOr(15.0);
        double rainMm = SmoothMath.softplus(cell.getRainfallOr(500.0), 0.0, 5.0);
        double biomassNat = SmoothMath.softplus(cell.getBiomassNaturalOr(0.0), 0.0, 10.0);

        double landWeight = SmoothMath.sigmoid(elev, 0.0, 5.0);

        // Ocean albedo with smooth sea-ice formation below -1.8 C
        double seaIce = SmoothMath.sigmoid(-tempC, 1.8, 1.0);
        double alphaOcean = 0.06 * (1.0 - seaIce) + 0.65 * seaIce;

        // Land snow cover and vegetative canopy cover fractions
        double fSnow = SmoothMath.sigmoid(-tempC, 0.0, 1.25);
        double fVeg = 0.90 * Math.tanh(biomassNat / 2800.0);

        double alphaSoil = 0.15 + 0.25 * Math.exp(-0.003 * rainMm);
        double alphaVeg = 0.14;
        double alphaSnow = 0.80;

        double alphaLand = (1.0 - fSnow) * (fVeg * alphaVeg + (1.0 - fVeg) * alphaSoil) + fSnow * alphaSnow;
        double blended = landWeight * alphaLand + (1.0 - landWeight) * alphaOcean;
        double computed = SmoothMath.smoothClamp(blended, 0.06, 0.85, 50.0);
        cell.setDynamicAlbedo(computed);
        return computed;
    }

    /**
     * Updates the detailed 7-segment Lotka (1907) stable age pyramid from total population,
     * life expectancy \(e_0\) ({@code lifespan}), and total fertility rate ({@code fertility}).
     *
     * <p>Replaces legacy technology-indexed interpolation with authentic demographic transition
     * coordinates: longevity survival index \(\ell = \sigma(e_0; 52, 12)\) and fertility moderation
     * index \(f = \sigma(4.2 - \text{TFR}; 0, 1.1)\).</p>
     *
     * @param cell target H3 cell
     */
    public static void updateAgePyramidFromObservables(H3Cell cell) {
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

        double lifespan = cell.getLifespanOr(38.0);
        double tfr = cell.getFertilityOr(5.2);
        double longevityProgress = SmoothMath.sigmoid(lifespan, 52.0, 12.0);
        double lowFertilityProgress = SmoothMath.sigmoid(4.2 - tfr, 0.0, 1.1);
        double dtFactor = 0.55 * longevityProgress + 0.45 * lowFertilityProgress;

        double share0to4   = 0.15 - (dtFactor * 0.10);
        double share5to14  = 0.27 - (dtFactor * 0.14);
        double share15to24 = 0.18 - (dtFactor * 0.04);
        double share25to49 = 0.25 + (dtFactor * 0.09);
        double share50to64 = 0.09 + (dtFactor * 0.07);
        double share65to79 = 0.05 + (dtFactor * 0.09);

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
     * Legacy adapter updating demographic age pyramid with explicit technology parameter.
     *
     * @param cell target H3 cell
     * @param techLevel technology level
     */
    public static void updateAgePyramidFromTotal(H3Cell cell, double techLevel) {
        updateAgePyramidFromObservables(cell);
    }

    /**
     * Calculates the total annual metabolic subsistence energy required by the cell's population in \(\text{GJ/year}\):
     * $$E_{\text{metabolic}} = \left(\sum_{c=0}^{6} N_c \cdot e_c\right) \cdot \mu_{\text{PAL}}(K/N, P_{\text{exo}}/N) \cdot \mu_{\text{species}}$$
     *
     * @param cell target H3 cell
     * @return annual caloric requirement in Gigajoules (\(\text{GJ}\))
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
        double mechPowerWatts = PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(cell);
        // Smooth physical mechanization reducing somatic Physical Activity Level (PAL) burden (FAO/WHO/UNU 2004)
        double mechanizationFactor = 0.50 * Math.tanh(capPerCapita / 60.0)
                + 0.50 * SmoothMath.sigmoid(mechPowerWatts, 400.0, 150.0);
        double extractiveLoad = 0.08 * SmoothMath.sigmoid(
                cell.getResourceMetalOr(0.0) * 5.0 + cell.getResourceCoalOr(0.0), 50.0, 20.0);
        double laborMultiplier = 1.25 - (0.30 * mechanizationFactor) + extractiveLoad * (1.0 - mechanizationFactor);

        double speciesMultiplier = switch (cell.getHomininSpecies()) {
            case 2 -> 1.40; // Homo Neanderthalensis (Churchill 2014 cold-adapted BMR)
            case 3 -> 1.20; // Denisovan / Archaic
            default -> 1.0; // Homo Sapiens
        };

        return totalPop * basePerCapitaGJ * laborMultiplier * speciesMultiplier;
    }

    /**
     * Standard average H3 hexagon area in \(\text{km}^2\) according to resolution level (\(0\) to \(15\)).
     *
     * @param resolution H3 resolution level (\(0\dots 15\))
     * @return average hexagon area in \(\text{km}^2\)
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
     * Calculates the effective 3D real surface area of the cell in \(\text{km}^2\), accounting for
     * embedded H3 resolution and smooth topographical slope expansion:
     * $$A_{3\text{D}} = A_{\text{H3}}(r) \cdot \sqrt{1 + 0.15 \cdot \operatorname{softplus}(f_{\text{move}}; 1, 0.2)^{1.8}}$$
     *
     * @param cell target H3 cell
     * @return effective 3D surface area in \(\text{km}^2\)
     */
    public static double getEffectiveSurfaceAreaKm2(H3Cell cell) {
        int res = 4;
        Long h3Index = cell.getH3Index();
        if (h3Index != null && h3Index != 0) {
            int mode = (int) ((h3Index >>> 59) & 0xF);
            int r = (int) ((h3Index >>> 52) & 0xF);
            if (mode == 1 && r >= 0 && r <= 15) {
                res = r;
            } else if (r >= 1 && r <= 15) {
                res = r;
            }
        }
        double baseArea = getAvgHexagonAreaKm2(res);
        double friction = cell.getMovementFrictionOr(1.0);
        double excessFriction = SmoothMath.softplus(friction, 1.0, 0.20);
        double slopeFactor3D = Math.sqrt(1.0 + 0.15 * Math.pow(excessFriction, 1.8));
        return baseArea * slopeFactor3D;
    }

    /**
     * Rescales all 7 demographic cohorts proportionally to match target total population,
     * maintaining strict demographic mass closure: \(\sum_i N_i = N_{\text{target}}\).
     *
     * @param cell      target H3 cell
     * @param targetPop new total population
     */
    public static void synchronizeCohortsToPopulation(H3Cell cell, int targetPop) {
        if (cell == null) return;
        if (targetPop <= 0) {
            cell.setPopulation(0);
            cell.setPop0to4(0);
            cell.setPop5to14(0);
            cell.setPop15to24(0);
            cell.setPop25to49(0);
            cell.setPop50to64(0);
            cell.setPop65to79(0);
            cell.setPop80Plus(0);
            return;
        }
        int cur0 = cell.getPop0to4Or(0);
        int cur5 = cell.getPop5to14Or(0);
        int cur15 = cell.getPop15to24Or(0);
        int cur25 = cell.getPop25to49Or(0);
        int cur50 = cell.getPop50to64Or(0);
        int cur65 = cell.getPop65to79Or(0);
        int cur80 = cell.getPop80PlusOr(0);
        long currentSum = (long) cur0 + cur5 + cur15 + cur25 + cur50 + cur65 + cur80;
        if (currentSum <= 0 || (currentSum < targetPop * 0.50 && (cur0 == 0 || cur25 == 0))) {
            cell.setPopulation(targetPop);
            updateAgePyramidFromObservables(cell);
            return;
        }
        double ratio = (double) targetPop / currentSum;
        int n0 = (int) Math.round(cur0 * ratio);
        int n5 = (int) Math.round(cur5 * ratio);
        int n15 = (int) Math.round(cur15 * ratio);
        int n25 = (int) Math.round(cur25 * ratio);
        int n50 = (int) Math.round(cur50 * ratio);
        int n65 = (int) Math.round(cur65 * ratio);
        int allocated = n0 + n5 + n15 + n25 + n50 + n65;
        int n80 = Math.max(0, targetPop - allocated);
        cell.setPop0to4(n0);
        cell.setPop5to14(n5);
        cell.setPop15to24(n15);
        cell.setPop25to49(n25);
        cell.setPop50to64(n50);
        cell.setPop65to79(n65);
        cell.setPop80Plus(n80);
        cell.setPopulation(targetPop);
    }

    // --- Delegated Snapshot & Data-Oriented Design (DOD) Projections ---

    /** @param src source cell @return deep cloned snapshot */
    public static H3Cell snapshot(H3Cell src) { return H3CellStateSnapshotHelper.snapshot(src); }
    /** @param cell source cell @return immutable climate state */
    public static ClimateState extractClimateState(H3Cell cell) { return H3CellStateSnapshotHelper.extractClimateState(cell); }
    /** @param cell target cell @param state climate state to apply */
    public static void applyClimateState(H3Cell cell, ClimateState state) { H3CellStateSnapshotHelper.applyClimateState(cell, state); }
    /** @param cell source cell @return immutable biomass state */
    public static BiomassState extractBiomassState(H3Cell cell) { return H3CellStateSnapshotHelper.extractBiomassState(cell); }
    /** @param cell target cell @param state biomass state to apply */
    public static void applyBiomassState(H3Cell cell, BiomassState state) { H3CellStateSnapshotHelper.applyBiomassState(cell, state); }
    /** @param cell source cell @return immutable demographic state */
    public static DemographicState extractDemographicState(H3Cell cell) { return H3CellStateSnapshotHelper.extractDemographicState(cell); }
    /** @param cell target cell @param state demographic state to apply */
    public static void applyDemographicState(H3Cell cell, DemographicState state) { H3CellStateSnapshotHelper.applyDemographicState(cell, state); }
    /** @param cell source cell @return immutable mineral state */
    public static MineralState extractMineralState(H3Cell cell) { return H3CellStateSnapshotHelper.extractMineralState(cell); }
    /** @param cell target cell @param state mineral state to apply */
    public static void applyMineralState(H3Cell cell, MineralState state) { H3CellStateSnapshotHelper.applyMineralState(cell, state); }
}
