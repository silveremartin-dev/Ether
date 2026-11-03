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

        // 1. Exact Canonical Metric Dispatch (Eliminates greedy substring collisions)
        switch (metricKey) {
            case "worldPopulation", "population" -> {
                return (double) pop;
            }
            case "grossWorldProduct" -> {
                return computeGrossWorldProduct(pop, tech);
            }
            case "primaryEnergy" -> {
                return computePrimaryEnergyEJ(pop, tech);
            }
            case "urbanizationRate" -> {
                return computeUrbanizationRate(tech);
            }
            case "literacyRate" -> {
                return computeLiteracyRate(tech);
            }
            case "currencyDebasement" -> {
                return computeCurrencyDebasement(tech, stability);
            }
            case "eliteOverproductionIndex" -> {
                return computeEliteOverproductionIndex(tech, stability);
            }
            case "realUnskilledWage" -> {
                return computeRealUnskilledWageIndex(tech);
            }
            case "politicalStressIndex" -> {
                return computePoliticalStressIndex(tech, stability);
            }
            case "asabiyyahSocialCohesion" -> {
                return computeAsabiyyahSocialCohesion(tech, stability);
            }
            case "sociopoliticalInstability" -> {
                return computeSociopoliticalInstability(tech, stability);
            }
            case "giniInequality" -> {
                return computeGiniInequality(tech);
            }
            case "sovereignDebtBurden" -> {
                return computeSovereignDebtBurden(tech);
            }
            case "globalTradeVolume" -> {
                return computeGlobalTradeVolume(tech);
            }
            case "soilErosionRate" -> {
                return computeTopsoilErosionLoss(tech);
            }
            case "agriculturalEroei" -> {
                return computeAgriculturalEroei(tech);
            }
            case "deforestationRate" -> {
                return computeForestCoverRemaining(tech);
            }
            case "informationSpeed" -> {
                return computeInformationTravelSpeed(tech);
            }
            case "megafaunaIndex" -> {
                return ProceduralPopulationEngine.calculateMegafaunaAbundanceIndex(year, (double) pop / 1e6, 0.2);
            }
            case "milankovitchInsolation" -> {
                return ProceduralPopulationEngine.calculateMilankovitchSummerInsolation65N(year);
            }
            case "zeroContainmentScore" -> {
                return computeZeroContainmentScore(year);
            }
            case "lifeExpectancyAtBirth" -> {
                return computeLifeExpectancy(tech, food);
            }
            case "metalSmeltingPerCapita" -> {
                return computeMetalSmeltingPerCapita(tech);
            }
            case "fossilSharePrimaryEnergy" -> {
                return computeFossilSharePrimaryEnergy(tech);
            }
            case "shippingFreightCostIndex" -> {
                return computeShippingFreightCostIndex(tech);
            }
            case "co2Concentration" -> {
                return computeCo2Concentration(tech, year);
            }
            case "temperatureAnomaly" -> {
                return 0.0;
            }
        }

        String lower = metricKey.toLowerCase();

        // 2. Unambiguous Substring Matching Fallback (Order-safe for composite labels)
        if (lower.contains("elite") || lower.contains("overproduction")) {
            return computeEliteOverproductionIndex(tech, stability);
        }
        if (lower.contains("fossil")) {
            return computeFossilSharePrimaryEnergy(tech);
        }
        if (lower.equals("population") || lower.equals("worldpopulation")) {
            return (double) pop;
        }
        if (lower.contains("grossworldproduct") || lower.equals("gdp") || lower.equals("gwp") || lower.equals("product")) {
            return computeGrossWorldProduct(pop, tech);
        }
        if (lower.contains("primaryenergy") || lower.equals("energy") || lower.equals("primary_energy")) {
            return computePrimaryEnergyEJ(pop, tech);
        }

        // Urbanization & Literacy
        if (lower.contains("urbanization")) {
            return computeUrbanizationRate(tech);
        }
        if (lower.contains("literacy")) {
            return computeLiteracyRate(tech);
        }

        // Institutional & Turchin SDT Metrics
        if (lower.contains("currency") || lower.contains("debasement")) {
            return computeCurrencyDebasement(tech, stability);
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

        // Trade, Transport & Environmental Energetics
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
        if (lower.contains("freight") || lower.contains("shipping")) {
            return computeShippingFreightCostIndex(tech);
        }
        if (lower.contains("life") || lower.contains("expectancy")) {
            return computeLifeExpectancy(tech, food);
        }

        // Paleolithic & Earth-System Cycles
        if (lower.contains("megafauna")) {
            return ProceduralPopulationEngine.calculateMegafaunaAbundanceIndex(year, (double) pop / 1e6, 0.2);
        }
        if (lower.contains("milankovitch")) {
            return ProceduralPopulationEngine.calculateMilankovitchSummerInsolation65N(year);
        }
        if (lower.contains("containment")) {
            return computeZeroContainmentScore(year);
        }
        if (lower.contains("co2")) {
            return computeCo2Concentration(tech, year);
        }
        if (lower.contains("temperature") || lower.contains("anom")) {
            return 0.0;
        }

        return 0.0;
    }

    // --- ANALYTICAL CLOSED-FORM POST-HOC EVALUATORS ---

    public static double computeCo2Concentration(double tech, int year) {
        if (year < -10000) {
            // EPICA Dome C Ice Core: Glacial minimum ~190 ppm at LGM (-20k BP), rising during deglaciation
            double deglaciationProgress = Math.clamp((year + 20000.0) / 10000.0, 0.0, 1.0);
            return 190.0 + deglaciationProgress * 85.0; // 190 ppm at -20k, 275 ppm at -10k
        }
        if (tech > 3.65) {
            // Industrial fossil emissions (Keeling Curve / NOAA GML):
            // ~283 ppm in 1800 -> ~301 ppm in 1914 -> ~425 ppm in 2026
            return 280.0 + 145.0 / (1.0 + Math.exp(-2.20 * (tech - 4.60)));
        }
        return 280.0;
    }

    public static double computeZeroContainmentScore(int year) {
        if (year <= -10000) {
            // Pleistocene / LGM: strict human containment before trans-oceanic navigation and Holocene dispersal
            return 1.0;
        }
        if (year <= 1492) {
            // Pre-Columbian separation of Afro-Eurasian and American biotas
            return Math.clamp(1.0 - (year + 10000.0) / 11492.0 * 0.25, 0.75, 1.0);
        }
        // Columbian Exchange (Crosby 1972) to modern global biological homogenization
        return Math.clamp(0.75 - (year - 1492.0) / 534.0 * 0.71, 0.04, 0.75);
    }

    public static double computeGrossWorldProduct(long pop, double tech) {
        // Maddison Project Database (2020) per-capita scaling:
        // Pre-industrial baseline (~175-195 Int$ global aggregate)
        // 1800 CE (~650-680 Int$ global aggregate)
        // 1914 CE (~1,450-1,530 Int$ global aggregate)
        // Modern 2026 (~13,500-14,000 Int$ global aggregate)
        double preIndustrial = 175.0 + 20.0 * Math.tanh(Math.max(0.0, tech - 1.50) * 0.90);
        // Stage 1: First and Second Industrial Revolution (coal, steam, rail, Bessemer steel)
        double industrialTakeoff = (tech > 3.30) ? 1800.0 / (1.0 + Math.exp(-2.20 * (tech - 4.10))) : 0.0;
        // Stage 2: Post-WWII & 20th/21st century technological acceleration (electrification, petrochem, digital)
        double modernAcceleration = (tech > 4.60) ? 11500.0 / (1.0 + Math.exp(-1.80 * (tech - 7.50))) : 0.0;
        double gdpPerCap = preIndustrial + industrialTakeoff + modernAcceleration;
        return ((double) pop * gdpPerCap) / 1e9; // Billion 1990 Int$
    }

    public static double computePrimaryEnergyEJ(long pop, double tech) {
        // Vaclav Smil (2017) 'Energy and Civilization' per-capita primary energy harvest:
        // Pre-industrial biomass/muscle (~15.2-16.0 GJ/capita)
        // 1800 CE (~17.5-18.5 GJ/capita)
        // 1914 CE (~32.0-35.0 GJ/capita)
        // Modern 2026 (~76.5-80.0 GJ/capita)
        double preIndustrialGJ = 15.2 + 0.8 * Math.tanh(Math.max(0.0, tech - 1.50) * 0.80);
        double fossilBoomGJ = (tech > 3.30) ? 61.0 / (1.0 + Math.exp(-2.10 * (tech - 4.85))) : 0.0;
        double gigajoulesPerCap = preIndustrialGJ + fossilBoomGJ;
        return ((double) pop * gigajoulesPerCap * 1e9) / 1e18; // Exajoules
    }

    /**
     * Calibrated Urbanization Rate (%):
     * Follows Bairoch (1988), De Vries (1984), and HYDE 3.4 empirical baselines.
     * Transitions smoothly from agrarian urban clusters (~5-8%) to industrial urbanization (~22% in 1914, ~56% modern).
     */
    public static double computeUrbanizationRate(double tech) {
        double techActivation = Math.tanh(Math.max(0.0, tech - 1.55) * 1.15);
        if (techActivation <= 0.0) return 0.0;

        // Agrarian baseline: 4% to 8%
        double agrarian = 4.0 + 3.5 * Math.tanh(Math.max(0.0, tech - 1.80) * 0.90);
        // Industrial transition: additional urban agglomeration reaching ~22% in 1914 and ~56% modern
        double industrialUrban = 48.0 / (1.0 + Math.exp(-2.20 * (tech - 4.75)));

        return Math.clamp(techActivation * (agrarian + industrialUrban), 0.0, 90.0);
    }

    /**
     * Calibrated Literacy Rate (%):
     * Follows Buringh & van Zanden (2009) and Graff (1987).
     * Ancient/Medieval scribes (~2-5%) -> Print vernacular (~10-15%) -> Mass education (~27% in 1914, >85% modern).
     */
    public static double computeLiteracyRate(double tech) {
        double techActivation = Math.tanh(Math.max(0.0, tech - 1.65) * 1.25);
        if (techActivation <= 0.0) return 0.0;

        double scribal = 2.5 + 3.5 * Math.tanh(Math.max(0.0, tech - 2.00) * 0.85);
        double massLiteracy = 80.0 / (1.0 + Math.exp(-2.20 * (tech - 4.65)));

        return Math.clamp(techActivation * (scribal + massLiteracy), 0.0, 99.0);
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
        double stress = Math.clamp(1.0 - (stability / 100.0), 0.0, 1.0);
        // Turchin (2016) Structural-Demographic Theory:
        // Baseline equilibrium = 1.0, rising during elite expansion & immiseration up to 2.5+
        double overproduction = 1.0 + 1.20 * stress + 0.35 * Math.tanh(Math.max(0.0, tech - 2.50));
        return Math.clamp(stateActivation * overproduction, 0.0, 5.0);
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
        double tradeActivation = Math.tanh(Math.max(0.0, tech - 1.60) * 1.25);
        if (tradeActivation <= 0.0) return 0.0;
        // Federico & Tena-Junguito (2017) 'World trade, 1800-1938':
        // Antiquity/Medieval: ~3-10
        // 1800 CE: ~45
        // 1914 CE: ~265
        // Modern: ~2500
        double preIndustrialTrade = 2.5 + 8.5 * Math.tanh(Math.max(0.0, tech - 2.00) * 0.85);
        double globalShippingBoom = 250.0 / (1.0 + Math.exp(-2.40 * (tech - 4.45)));
        double hyperGlobalization = (tech > 4.8) ? 2200.0 / (1.0 + Math.exp(-2.80 * (tech - 5.50))) : 0.0;
        return Math.clamp(tradeActivation * (preIndustrialTrade + globalShippingBoom + hyperGlobalization), 0.0, 3000.0);
    }

    public static double computeTopsoilErosionLoss(double tech) {
        double lossActivation = Math.tanh(Math.max(0.0, tech - 1.20) * 1.50);
        if (lossActivation <= 0.0) return 0.03;
        // Montgomery (2007) 'Dirt: The Erosion of Civilizations':
        // Natural geological baseline: ~0.03 - 0.10 t/ha/yr
        // Traditional plowing/grazing (Antiquity/Medieval): ~12-16 t/ha/yr
        // Industrial mechanized monoculture: ~30-45 t/ha/yr
        double traditionalErosion = 14.5 * Math.tanh(Math.max(0.0, tech - 1.35) * 1.20);
        double mechanizedErosion = 20.0 / (1.0 + Math.exp(-1.80 * (tech - 3.80)));
        return Math.clamp(0.03 + lossActivation * (traditionalErosion + mechanizedErosion), 0.03, 50.0);
    }

    public static double computeAgriculturalEroei(double tech) {
        double agActivation = Math.tanh(Math.max(0.0, tech - 1.20) * 3.0);
        if (agActivation <= 0.0) return 0.0;
        // Hall & Klitgaard (2012) / Leach (1976): Traditional agriculture ~10-15 EROEI
        // falling to ~1.5 - 2.5 with chemical/fossil mechanization (tech > 3.5)
        double traditionalEroei = 14.5 - 10.0 / (1.0 + Math.exp(-1.40 * (tech - 2.50)));
        return Math.clamp(agActivation * traditionalEroei, 0.0, 20.0);
    }

    public static double computeForestCoverRemaining(double tech) {
        // Williams (2003) 'Deforesting the Earth' & Kaplan et al. (2009):
        // Global forest cover remaining (% of pristine post-glacial baseline):
        // Pre-agricultural Paleolithic / Early Neolithic: ~98-99%
        // Classical Antiquity / Middle Ages: ~85-92%
        // 19th-20th Century Industrial Deforestation: ~65-75%
        double clearingActivation = Math.tanh(Math.max(0.0, tech - 1.35) * 0.90);
        double traditionalClearing = 12.0 * clearingActivation;
        double industrialClearing = (tech > 3.0) ? 22.0 / (1.0 + Math.exp(-1.80 * (tech - 3.80))) : 0.0;
        double clearing = traditionalClearing + industrialClearing;
        return Math.clamp(99.0 - clearing, 50.0, 99.0);
    }

    /**
     * Calibrated Information Travel Speed (km/day):
     * Courier/horse (~30-90 km/day) -> Steam/rail/optical telegraph (~250-1,200 km/day)
     * -> Victorian electrical telegraph / early radio (~10^6 km/day)
     * -> Modern global telecommunications / fiber optics (Standage 1998, Scheidel 2014).
     */
    public static double computeInformationTravelSpeed(double tech) {
        double courierSpeed = 4.0 + 95.0 * Math.tanh(Math.max(0.0, tech - 1.35) * 0.85);
        double telegraphSpeed = 24000.0 / (1.0 + Math.exp(-2.50 * (tech - 3.85)));
        double earlyTelecom = (tech > 4.20) ? 5.8e6 * Math.tanh(Math.max(0.0, tech - 4.20) * 2.50) : 0.0;
        double modernTelecom = (tech > 4.80) ? 2.94e8 / (1.0 + Math.exp(-3.50 * (tech - 5.50))) : 0.0;
        return Math.clamp(courierSpeed + telegraphSpeed + earlyTelecom + modernTelecom, 4.0, 3.0e8);
    }

    public static double computeMetalSmeltingPerCapita(double tech) {
        double smeltActivation = Math.tanh(Math.max(0.0, tech - 1.65) * 1.30);
        if (smeltActivation <= 0.0) return 0.0;
        // Malandra (1990) / USGS (2023): Antiquity (~1.5 kg) -> Medieval (~3.0 kg)
        // -> 1800 CE (~10 kg) -> 1914 CE (~57 kg) -> 2026 CE (~255 kg)
        double preIndustrial = 1.2 + 3.0 * Math.tanh(Math.max(0.0, tech - 2.00) * 0.80);
        double industrialBessemer = 245.0 / (1.0 + Math.exp(-2.20 * (tech - 4.80)));
        return Math.clamp(smeltActivation * (preIndustrial + industrialBessemer), 0.0, 350.0);
    }

    /**
     * Calibrated Fossil Fuel Share in Primary Energy (%):
     * Strictly inactive before the Industrial Revolution (T < 3.40), eliminating early era leakage.
     */
    public static double computeFossilSharePrimaryEnergy(double tech) {
        double industrialActivation = Math.tanh(Math.max(0.0, tech - 3.40) * 1.80);
        if (industrialActivation <= 0.0) return 0.0;
        // Smil (2017) / Ritchie (2020):
        // Pre-industrial: 0% fossil share
        // 1800 CE: ~2-5% (early coal)
        // 1914 CE: ~60-70% (coal & early oil)
        // 2026 CE: ~80-82%
        double transition = 82.0 / (1.0 + Math.exp(-2.50 * (tech - 4.10)));
        return Math.clamp(industrialActivation * transition, 0.0, 95.0);
    }

    public static double computeShippingFreightCostIndex(double tech) {
        double tradeActivation = Math.tanh(Math.max(0.0, tech - 1.55) * 1.50);
        if (tradeActivation <= 0.0) return 0.0;
        // Mohammed & Williamson (2004) / Harley (1988):
        // Antiquity/Rome (~1420) -> Renaissance lateen/caravel/fluyt (~930 in 1347, ~450 in 1700)
        // -> 19th Century Steam (~250 in 1800, ~100 in 1914, ~20 modern)
        double preModernDecline = 1450.0 - 800.0 * Math.tanh(Math.max(0.0, tech - 2.50) * 0.85);
        double steamRevolution = 550.0 / (1.0 + Math.exp(-2.20 * (tech - 4.10)));
        return Math.clamp(tradeActivation * (preModernDecline - steamRevolution), 20.0, 1600.0);
    }

    public static double computeLifeExpectancy(double tech, double food) {
        double nutritionBonus = Math.clamp((food - 2000.0) / 1000.0, -5.0, 5.0);
        // Omran (1971) Epidemiological Transition / Riley (2005):
        // Paleolithic/Agrarian baseline (~26-32 yrs), 1800 CE (~32-35 yrs), 1914 CE (~38-44 yrs), Modern (>72 yrs)
        double baselineLife = 27.5 + 4.5 * Math.tanh(Math.max(0.0, tech - 1.50) * 0.70);
        double modernMedicine = 42.0 / (1.0 + Math.exp(-2.20 * (tech - 4.65)));
        return Math.clamp(baselineLife + modernMedicine + nutritionBonus, 20.0, 85.0);
    }
}
