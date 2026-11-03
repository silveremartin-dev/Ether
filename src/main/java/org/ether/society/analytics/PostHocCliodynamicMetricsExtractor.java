/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.analytics.SimulationRunRecord.MetricSnapshot;
import org.ether.society.engines.tier2.theories.ProceduralPopulationEngine;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * <h1>Post-Hoc Cliodynamic Metrics Extractor</h1>
 * <p>
 * Decouples physical simulation execution from cliodynamic academic evaluation.<br>
 * Evaluates macro-historical and cliodynamic indicators (Seshat complexity, EROEI,
 * Turchin structural-demographic stress, Marchetti fossil shares, ORBIS speeds)
 * <b>strictly a posteriori</b> from saved physical snapshots, eliminating all heavy
 * telemetry computation from the headless simulation loop.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class PostHocCliodynamicMetricsExtractor {

    private PostHocCliodynamicMetricsExtractor() {
        // Utility static class
    }

    /**
     * Evaluates a single cliodynamic metric a posteriori for a given snapshot.
     *
     * @param snap metric snapshot containing fundamental physical variables
     * @param metricKey canonical metric identifier
     * @param year historical calendar year
     * @return evaluated cliodynamic metric value
     */
    public static double computeMetric(MetricSnapshot snap, String metricKey, int year) {
        if (snap == null || metricKey == null) return 0.0;

        // Check if value was already explicitly recorded (e.g. in live GUI mode)
        double preRecorded = snap.getValue(metricKey);
        if (preRecorded != 0.0 && !Double.isNaN(preRecorded)) {
            return preRecorded;
        }

        long pop = snap.getPopulation();
        double tech = snap.getAvgTech();
        double stability = snap.getStability();
        double food = snap.getFood();

        String lower = metricKey.toLowerCase();

        // 1. Demography & Macro-Production
        if (lower.equals("population") || lower.equals("worldpopulation")) {
            return (double) pop;
        }
        if (lower.contains("grossworldproduct") || lower.equals("gdp") || lower.contains("product")) {
            return computeGrossWorldProduct(pop, tech);
        }
        if (lower.contains("primaryenergy") || lower.contains("energy")) {
            return computePrimaryEnergyEJ(pop, tech);
        }

        // 2. Urbanization & Literacy
        if (lower.contains("urbanization")) {
            return computeUrbanizationRate(tech);
        }
        if (lower.contains("literacy")) {
            return computeLiteracyRate(tech);
        }

        // 3. Institutional & Turchin SDT Metrics
        if (lower.contains("currency") || lower.contains("debasement")) {
            return computeCurrencyDebasement(tech, stability);
        }
        if (lower.contains("elite") || lower.contains("overproduction")) {
            return computeEliteOverproductionIndex(tech, stability);
        }
        if (lower.contains("wage") || lower.contains("salaire")) {
            return computeRealUnskilledWageIndex(tech);
        }
        if (lower.contains("politicalstress") || lower.contains("psi")) {
            return computePoliticalStressIndex(tech, stability);
        }
        if (lower.contains("asabiyyah") || lower.contains("cohesion")) {
            return computeAsabiyyahSocialCohesion(tech, stability);
        }
        if (lower.contains("instability") || lower.contains("conflict")) {
            return computeSociopoliticalInstability(tech, stability);
        }
        if (lower.contains("gini")) {
            return computeGiniInequality(tech);
        }
        if (lower.contains("sovereigndebt") || lower.contains("debt")) {
            return computeSovereignDebtBurden(tech);
        }

        // 4. Trade, Transport & Environmental Energetics
        if (lower.contains("trade") || lower.contains("commerce")) {
            return computeGlobalTradeVolume(tech);
        }
        if (lower.contains("soil") || lower.contains("erosion")) {
            return computeTopsoilErosionLoss(tech);
        }
        if (lower.contains("eroei") || lower.contains("eroi")) {
            return computeAgriculturalEroei(tech);
        }
        if (lower.contains("deforest") || lower.contains("forest")) {
            return computeForestCoverRemaining(tech);
        }
        if (lower.contains("speed") || lower.contains("vitesse")) {
            return computeInformationTravelSpeed(tech);
        }
        if (lower.contains("smelt") || lower.contains("metal")) {
            return computeMetalSmeltingPerCapita(tech);
        }
        if (lower.contains("fossil")) {
            return computeFossilSharePrimaryEnergy(tech);
        }
        if (lower.contains("freight") || lower.contains("shipping")) {
            return computeShippingFreightCostIndex(tech);
        }
        if (lower.contains("life") || lower.contains("expectancy")) {
            return computeLifeExpectancy(tech, food);
        }

        // 5. Paleolithic & Earth-System Cycles
        if (lower.contains("megafauna")) {
            return ProceduralPopulationEngine.calculateMegafaunaAbundanceIndex(year, (double) pop / 1e6, 0.2);
        }
        if (lower.contains("milankovitch")) {
            return ProceduralPopulationEngine.calculateMilankovitchSummerInsolation65N(year);
        }
        if (lower.contains("containment")) {
            return 1.0;
        }
        if (lower.contains("co2")) {
            return 280.0 + (tech > 3.8 ? Math.max(0.0, (tech - 3.8) * 45.0) : 0.0);
        }
        if (lower.contains("temperature") || lower.contains("anom")) {
            return 0.0;
        }

        return 0.0;
    }

    // --- ANALYTICAL CLOSED-FORM POST-HOC EVALUATORS ---

    public static double computeGrossWorldProduct(long pop, double tech) {
        // Maddison Project Database (2020) per-capita scaling:
        // Pre-industrial baseline (~180 Int$ global aggregate), industrial takeoff post-1870 (~1,480 Int$ in 1914, ~13,000 Int$ modern)
        double gdpPerCap = 175.0 + 35.0 * Math.tanh(Math.max(0.0, tech - 1.50) * 0.90)
                + (tech > 3.70 ? 12800.0 / (1.0 + Math.exp(-2.30 * (tech - 5.00))) : 0.0);
        return ((double) pop * gdpPerCap) / 1e9; // Billion 1990 Int$
    }

    public static double computePrimaryEnergyEJ(long pop, double tech) {
        // Vaclav Smil (2017) per-capita primary energy harvest:
        // Pre-industrial biomass/muscle (~15.2 GJ/capita), coal/steam breakout (~29.5 GJ/capita in 1914, ~76.5 GJ/capita modern)
        double gigajoulesPerCap = 15.2 + 1.2 * Math.tanh(Math.max(0.0, tech - 1.50) * 0.80)
                + (tech > 3.60 ? 60.0 / (1.0 + Math.exp(-2.25 * (tech - 4.90))) : 0.0);
        return ((double) pop * gigajoulesPerCap * 1e9) / 1e18; // Exajoules
    }

    /**
     * Calibrated Urbanization Rate (%):
     * Follows Bairoch (1988), De Vries (1984), and HYDE 3.4 empirical baselines.
     * Transitions smoothly from agrarian urban clusters (~5-10%) to industrial urbanization (~30% in 1914, ~55% modern).
     */
    public static double computeUrbanizationRate(double tech) {
        double techActivation = Math.tanh(Math.max(0.0, tech - 1.55) * 1.15);
        if (techActivation <= 0.0) return 0.0;

        // Agrarian baseline: 5% to 10%
        double agrarianRate = 9.5 / (1.0 + Math.exp(-1.5 * (tech - 2.8)));
        // Industrial transition: additional urban agglomeration reaching ~20% in 1914 and ~45% in late 20th century
        double industrialRate = 55.0 / (1.0 + Math.exp(-2.2 * (tech - 4.60)));

        return Math.clamp(techActivation * Math.max(agrarianRate, industrialRate), 0.0, 92.0);
    }

    /**
     * Calibrated Literacy Rate (%):
     * Follows Buringh & van Zanden (2009) and Graff (1987).
     * Scribes (~1-5%) -> Print vernacular (~10-20%) -> Mass universal education (~40% in 1914, >85% modern).
     */
    public static double computeLiteracyRate(double tech) {
        double techActivation = Math.tanh(Math.max(0.0, tech - 1.55) * 1.25);
        if (techActivation <= 0.0) return 0.0;

        // Unified progression curve avoiding additive inflation
        double scribalVernacular = 18.0 / (1.0 + Math.exp(-1.4 * (tech - 3.10)));
        double modernMassSchooling = 82.0 / (1.0 + Math.exp(-2.1 * (tech - 4.65)));

        return Math.clamp(techActivation * Math.max(scribalVernacular, modernMassSchooling), 0.0, 99.0);
    }

    public static double computeCurrencyDebasement(double tech, double stability) {
        double mintingActivation = Math.tanh(Math.max(0.0, tech - 1.80) * 1.25);
        if (mintingActivation <= 0.0) return 0.0;
        double stress = Math.clamp(1.0 - (stability / 100.0), 0.0, 1.0);
        double debasement = (30.0 + 55.0 * Math.pow(stress, 1.45)) * mintingActivation;
        return Math.clamp(debasement, 0.0, 100.0);
    }

    public static double computeEliteOverproductionIndex(double tech, double stability) {
        double stateActivation = Math.tanh(Math.max(0.0, tech - 1.55) * 1.45);
        if (stateActivation <= 0.0) return 0.0;
        double inequalityProxy = Math.clamp(1.0 - (stability / 100.0), 0.0, 1.0);
        double overproduction = stateActivation * (1.0 + 1.45 * Math.max(0.0, inequalityProxy - 0.30)
                + 0.25 * Math.tanh(Math.max(0.0, tech - 2.50)));
        return Math.clamp(overproduction, 0.0, 10.0);
    }

    public static double computeRealUnskilledWageIndex(double tech) {
        // Allen (2001) / Clark (2005) Real Wage Index (100 = 1750-1800 level):
        // Pre-industrial Malthusian wage (~95-105), First Industrial break post-1870 (~210 in 1914, ~650 modern)
        double industrialBreakout = 550.0 / (1.0 + Math.exp(-2.20 * (tech - 4.60)));
        return Math.clamp(100.0 + industrialBreakout, 50.0, 1000.0);
    }

    public static double computePoliticalStressIndex(double tech, double stability) {
        double stateActivation = Math.tanh(Math.max(0.0, tech - 1.55) * 1.35);
        if (stateActivation <= 0.0) return 0.0;
        double stress = Math.clamp(1.0 - (stability / 100.0), 0.0, 1.0);
        return Math.clamp(stateActivation * (stress * 100.0), 0.0, 100.0);
    }

    public static double computeAsabiyyahSocialCohesion(double tech, double stability) {
        double stateDecayFactor = Math.tanh(Math.max(0.0, tech - 1.55) * 0.65);
        double inequality = Math.clamp(1.0 - (stability / 100.0), 0.0, 1.0);
        double asabiyyah = 1.02 - 0.35 * stateDecayFactor - 0.65 * inequality;
        return Math.clamp(asabiyyah, 0.01, 1.0);
    }

    public static double computeSociopoliticalInstability(double tech, double stability) {
        return computePoliticalStressIndex(tech, stability);
    }

    public static double computeGiniInequality(double tech) {
        double stateActivation = Math.tanh(Math.max(0.0, tech - 1.55) * 1.20);
        double agrarianGini = 0.18 + 0.32 * stateActivation;
        double kuznetsCurve = 0.15 * Math.exp(-Math.pow((tech - 4.2), 2) / 0.8);
        return Math.clamp(agrarianGini + kuznetsCurve, 0.18, 0.75);
    }

    public static double computeSovereignDebtBurden(double tech) {
        double fiscalStateActivation = Math.tanh(Math.max(0.0, tech - 1.65) * 1.15);
        if (fiscalStateActivation <= 0.0) return 0.0;
        // Reinhart & Rogoff (2009) / Dickson (1967): Medieval-Renaissance public debt (~10-30%)
        // expanding to modern fiscal debt states (~75% in 1815/1914, ~100% post-1945/2020)
        double earlyDebt = 8.0 + 18.0 * Math.tanh(Math.max(0.0, tech - 2.20) * 1.10);
        double modernFiscalState = 80.0 / (1.0 + Math.exp(-2.20 * (tech - 3.85)));
        return Math.clamp(fiscalStateActivation * Math.max(earlyDebt, modernFiscalState), 0.0, 250.0);
    }

    public static double computeGlobalTradeVolume(double tech) {
        double tradeActivation = Math.tanh(Math.max(0.0, tech - 1.55) * 1.20);
        if (tradeActivation <= 0.0) return 0.0;
        // Federico & Tena-Junguito (2017): Antiquity (~3-4) -> Middle Ages (~11.6 in 1347)
        // -> Atlantic Trade (~45 in 1800) -> 19th Century Globalization (~265 in 1914) -> Modern (~2500)
        double preIndustrialTrade = 1.0 + 22.0 * Math.tanh(Math.max(0.0, tech - 2.00) * 0.90);
        double globalShippingBoom = 250.0 / (1.0 + Math.exp(-2.40 * (tech - 4.15)));
        double hyperGlobalization = (tech > 4.5) ? 2200.0 / (1.0 + Math.exp(-2.80 * (tech - 5.20))) : 0.0;
        return Math.clamp(tradeActivation * (preIndustrialTrade + globalShippingBoom + hyperGlobalization), 0.0, 3000.0);
    }

    public static double computeTopsoilErosionLoss(double tech) {
        double lossActivation = Math.tanh(Math.max(0.0, tech - 1.25) * 0.65);
        double mechanizedAcceleration = 1.0 / (1.0 + Math.exp(-1.20 * (tech - 3.50)));
        double soilLoss = 0.03 + 32.0 * lossActivation * mechanizedAcceleration;
        return Math.clamp(soilLoss, 0.03, 80.0);
    }

    public static double computeAgriculturalEroei(double tech) {
        return 0.12 + 6.88 / (1.0 + Math.exp(1.35 * (tech - 2.80)));
    }

    public static double computeForestCoverRemaining(double tech) {
        double clearingActivation = Math.tanh(Math.max(0.0, tech - 1.25) * 0.70);
        double industrialDeforestation = 1.0 / (1.0 + Math.exp(-1.10 * (tech - 3.40)));
        double clearing = 36.0 * clearingActivation * industrialDeforestation;
        double postGlacialFactor = Math.tanh(Math.max(0.0, tech - 1.15) * 2.0);
        return Math.clamp(postGlacialFactor * (99.0 - clearing), 0.0, 100.0);
    }

    /**
     * Calibrated Information Travel Speed (km/day):
     * Courier/horse (~30-80 km/day) -> Steam/rail (~250-1,200 km/day) -> Telecommunications (>10^6 km/day).
     */
    public static double computeInformationTravelSpeed(double tech) {
        double courierSpeed = 4.0 + 76.0 * Math.tanh(Math.max(0.0, tech - 1.55) * 0.80);
        double railSteamSpeed = 2400.0 / (1.0 + Math.exp(-2.5 * (tech - 3.90)));
        double telecomSpeed = (tech > 4.15) ? 3.0e8 / (1.0 + Math.exp(-3.5 * (tech - 4.45))) : 0.0;
        return Math.clamp(courierSpeed + railSteamSpeed + telecomSpeed, 4.0, 3.0e8);
    }

    public static double computeMetalSmeltingPerCapita(double tech) {
        double smeltActivation = Math.tanh(Math.max(0.0, tech - 1.65) * 1.30);
        if (smeltActivation <= 0.0) return 0.0;
        // Malandra (1990) / USGS (2023): Antiquity (~1.5 kg) -> Medieval (~3.0 kg)
        // -> 1800 CE (~10 kg) -> 1914 CE (~57 kg) -> 2026 CE (~255 kg)
        double preIndustrial = 1.2 + 6.0 * Math.tanh(Math.max(0.0, tech - 2.00) * 0.80);
        double industrialBessemer = 250.0 / (1.0 + Math.exp(-2.20 * (tech - 4.60)));
        return Math.clamp(smeltActivation * (preIndustrial + industrialBessemer), 0.0, 350.0);
    }

    /**
     * Calibrated Fossil Fuel Share in Primary Energy (%):
     * Strictly inactive before the Industrial Revolution (T < 3.40), eliminating early era leakage.
     */
    public static double computeFossilSharePrimaryEnergy(double tech) {
        double industrialActivation = Math.tanh(Math.max(0.0, tech - 3.40) * 1.80);
        if (industrialActivation <= 0.0) return 0.0;
        double logisticCoalOil = 88.0 / (1.0 + Math.exp(-2.80 * (tech - 4.25)));
        return Math.clamp(industrialActivation * logisticCoalOil, 0.0, 90.0);
    }

    public static double computeShippingFreightCostIndex(double tech) {
        double tradeActivation = Math.tanh(Math.max(0.0, tech - 1.55) * 1.50);
        if (tradeActivation <= 0.0) return 0.0;
        // Harley (1988): Antiquity/Rome (~1420) -> Renaissance lateen/caravel/fluyt (~930 in 1347, ~450 in 1700)
        // -> 19th Century Steam (~250 in 1800, ~56 in 1914, ~20 modern)
        double preModernDecline = 1450.0 - 800.0 * Math.tanh(Math.max(0.0, tech - 2.50) * 0.85);
        double steamRevolution = 600.0 / (1.0 + Math.exp(-2.20 * (tech - 4.10)));
        return Math.clamp(tradeActivation * (preModernDecline - steamRevolution), 20.0, 1600.0);
    }

    public static double computeLifeExpectancy(double tech, double food) {
        // Omran (1971) Epidemiological Transition / Riley (2005):
        // Paleolithic/Agrarian baseline (~26-32 yrs), 1800 CE (~32-34 yrs), 1914 CE (~38-42 yrs), Modern (>72 yrs)
        double base = 26.0 + 6.0 * Math.tanh(Math.max(0.0, tech - 1.55) * 0.80);
        double modernMedicine = 46.0 / (1.0 + Math.exp(-2.20 * (tech - 4.85)));
        return Math.clamp(base + modernMedicine, 20.0, 85.0);
    }
}
