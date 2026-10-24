/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.analytics.HistoricalScenarioCalibrationHarness.CalibrationScenarioDefinition;
import org.ether.society.analytics.HistoricalScenarioCalibrationHarness.EmpireGroundTruth;

import java.util.List;

/**
 * Canonical Scenario Catalog for Historical Steady-Regime Calibration,
 * Counterfactual Falsifications, and Multi-Millennial Epoch Validation.
 *
 * <p>Extracted from {@link HistoricalScenarioCalibrationHarness} to satisfy the
 * Single Responsibility Principle (SRP).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-academic
 */
public final class HistoricalCalibrationScenarioCatalog {

    private HistoricalCalibrationScenarioCatalog() {
        // Pure static catalog
    }

    /**
     * Predefined canonical steady-regime calibration scenarios.
     */
    public static final List<CalibrationScenarioDefinition> CALIBRATION_SCENARIOS = List.of(
            new CalibrationScenarioDefinition(
                    "CLASSICAL_AGRARIAN_EXPANSION",
                    "Classical Antiquity Steady Agrarian Consolidation (-500 BCE -> 100 CE)",
                    -500, 100,
                    List.of(-400, -300, -200, -100, 0, 100),
                    "Stable agrarian empire expansion across Mediterranean and Han China prior to Antonine plague (165 CE).",
                    100.0, 180.0,
                    3.2f, 1200.0f, 0.85f,
                    List.of(
                            new EmpireGroundTruth("Roman Republic / Empire", 0, 41.9, 12.5, 54.0, 3.8, 3800000.0),
                            new EmpireGroundTruth("Han Dynasty China", 0, 34.2, 108.9, 58.0, 3.9, 4000000.0),
                            new EmpireGroundTruth("Maurya / Satavahana India", 0, 25.6, 85.1, 35.0, 3.5, 2500000.0)
                    ),
                    true
            ),
            new CalibrationScenarioDefinition(
                    "HIGH_MEDIEVAL_GROWTH",
                    "High Medieval Expansion & Land Clearance (1000 CE -> 1300 CE)",
                    1000, 1300,
                    List.of(1020, 1040, 1060, 1080, 1100, 1120, 1140, 1160, 1180, 1200, 1220, 1240, 1260, 1280, 1300),
                    "Medieval Warm Optimum, agricultural intensification (heavy plow, three-field rotation) before the 1347 Black Death.",
                    265.0, 400.0,
                    4.0f, 2200.0f, 0.95f,
                    List.of(
                            new EmpireGroundTruth("Song Dynasty China", 1100, 34.8, 114.3, 100.0, 5.0, 3100000.0),
                            new EmpireGroundTruth("Holy Roman Empire / Capetian France", 1100, 48.8, 2.3, 18.0, 4.2, 1200000.0),
                            new EmpireGroundTruth("Fatimid / Ayyubid Caliphate", 1100, 30.0, 31.2, 14.0, 4.5, 2000000.0)
                    ),
                    true
            ),
            new CalibrationScenarioDefinition(
                    "PRE_INDUSTRIAL_CONTINUITY",
                    "Early Modern Pre-Industrial Commercial Continuity (1500 CE -> 1750 CE)",
                    1500, 1750,
                    List.of(1520, 1540, 1560, 1580, 1600, 1620, 1640, 1660, 1680, 1700, 1720, 1740, 1750),
                    "Post-plague agrarian recovery, Columbian crop diffusion (maize/potato), proto-industrial trade before coal steam breakout.",
                    425.0, 710.0,
                    5.2f, 6500.0f, 1.25f,
                    List.of(
                            new EmpireGroundTruth("Ming / Qing Dynasty", 1700, 39.9, 116.4, 210.0, 6.2, 11000000.0),
                            new EmpireGroundTruth("Mughal Empire", 1700, 28.6, 77.2, 150.0, 5.8, 4000000.0),
                            new EmpireGroundTruth("Kingdom of France / British Empire", 1700, 48.8, 2.3, 21.5, 6.5, 2500000.0)
                    ),
                    true
            ),
            new CalibrationScenarioDefinition(
                    "SECOND_INDUSTRIAL_ACCELERATION",
                    "Late 19th Century Industrial & Thermodynamic Scaling (1850 CE -> 1910 CE)",
                    1850, 1910,
                    List.of(1860, 1870, 1880, 1890, 1900, 1910),
                    "Continuous coal, steam, railway, and steel expansion with steady demographic transition prior to World War I (1914).",
                    1260.0, 1750.0,
                    7.5f, 45000.0f, 2.20f,
                    List.of(
                            new EmpireGroundTruth("British Empire (Global)", 1900, 51.5, -0.1, 380.0, 8.8, 30000000.0),
                            new EmpireGroundTruth("German Empire & Central Europe", 1900, 52.5, 13.4, 56.0, 8.7, 540000.0),
                            new EmpireGroundTruth("United States", 1900, 38.9, -77.0, 76.0, 8.9, 7800000.0)
                    ),
                    true
            ),
            new CalibrationScenarioDefinition(
                    "POST_WAR_GOLDEN_AGE",
                    "Post-WWII Green Revolution & Exergy Growth (1950 CE -> 1990 CE)",
                    1950, 1990,
                    List.of(1950, 1960, 1970, 1980, 1990),
                    "Continuous demographic expansion, Haber-Bosch synthetic nitrogen, oil scaling (Trente Glorieuses) with ultra-high precision telemetry.",
                    2525.0, 5327.0,
                    8.8f, 120000.0f, 3.20f,
                    List.of(
                            new EmpireGroundTruth("United States & NATO", 1980, 38.9, -77.0, 227.0, 10.2, 9500000.0),
                            new EmpireGroundTruth("USSR & Warsaw Pact", 1980, 55.7, 37.6, 265.0, 9.8, 22400000.0),
                            new EmpireGroundTruth("People's Republic of China", 1980, 39.9, 116.4, 981.0, 8.9, 9600000.0)
                    ),
                    true
            )
    );

    /**
     * Counterfactual Falsification Twin Scenarios (Acute Bifurcations & Historical Ruptures).
     */
    public static final List<CalibrationScenarioDefinition> FALSIFICATION_COUNTERFACTUAL_SCENARIOS = List.of(
            // 1. Late Bronze Age Collapse (~1200 BCE)
            new CalibrationScenarioDefinition(
                    "BRONZE_AGE_COLLAPSE_1200BCE_UNFORCED",
                    "Late Bronze Age Collapse Twin A: Unforced Continuous Eastern Med Baseline (-1250 -> -1150 BCE)",
                    -1250, -1150,
                    List.of(-1250, -1220, -1200, -1180, -1150),
                    "Unforced agrarian expansion of Mycenaean, Hittite, and Ugarit palatial trade networks without mega-drought or Sea Peoples network collapse.",
                    50.0, 35.0,
                    2.8f, 1100.0f, 0.70f,
                    List.of(
                            new EmpireGroundTruth("Mycenaean Palaces & Ugarit", -1200, 37.5, 22.8, 4.5, 3.2, 120000.0),
                            new EmpireGroundTruth("Hittite Empire (Hattusa)", -1200, 40.0, 34.6, 3.8, 3.0, 450000.0),
                            new EmpireGroundTruth("New Kingdom Egypt (Ramesside)", -1200, 25.7, 32.6, 4.0, 3.1, 500000.0)
                    ),
                    false
            ),
            new CalibrationScenarioDefinition(
                    "BRONZE_AGE_COLLAPSE_1200BCE_COUNTERFACTUAL",
                    "Late Bronze Age Collapse Twin B: Counterfactual Resilient Agrarian Network (-1250 -> -1150 BCE)",
                    -1250, -1150,
                    List.of(-1250, -1220, -1200, -1180, -1150),
                    "Counterfactual simulation testing whether decentralized grain reserves and diversified bronze alloy networks prevent systemic palatial collapse.",
                    50.0, 58.0,
                    2.8f, 1100.0f, 0.85f,
                    List.of(
                            new EmpireGroundTruth("Resilient Mycenaean Federation", -1200, 37.5, 22.8, 5.2, 3.5, 150000.0),
                            new EmpireGroundTruth("Hittite Grain Reserve Network", -1200, 40.0, 34.6, 4.5, 3.3, 500000.0)
                    ),
                    false
            ),

            // 2. 536 CE Volcanic Dust Veil & Justinian Plague (530 -> 600 CE)
            new CalibrationScenarioDefinition(
                    "LATE_ANTIQUE_COOLING_536CE_UNFORCED",
                    "536 CE Volcanic Winter Twin A: Unforced Roman-Byzantine Agrarian Continuity (530 -> 600 CE)",
                    530, 600,
                    List.of(530, 536, 542, 560, 580, 600),
                    "Simulates Justinian Mediterranean reconquest and Sasanian dynamics without 536 CE Ilopango/Katla dust veil and without 541 CE Yersinia pestis pandemic.",
                    210.0, 185.0,
                    3.8f, 1800.0f, 0.88f,
                    List.of(
                            new EmpireGroundTruth("Byzantine Empire (Justinian)", 540, 41.0, 28.9, 32.0, 4.2, 3200000.0),
                            new EmpireGroundTruth("Sasanian Empire (Khosrow I)", 540, 33.3, 44.4, 20.0, 4.0, 3500000.0)
                    ),
                    false
            ),
            new CalibrationScenarioDefinition(
                    "LATE_ANTIQUE_COOLING_536CE_COUNTERFACTUAL",
                    "536 CE Volcanic Winter Twin B: Counterfactual Epidemic Containment & Grain Subsidy (530 -> 600 CE)",
                    530, 600,
                    List.of(530, 536, 542, 560, 580, 600),
                    "Tests counterfactual quarantine protocols at Pelusium port and Nile grain distribution to evaluate if Justinian Reconquest survives climatic stress.",
                    210.0, 230.0,
                    3.8f, 1800.0f, 0.95f,
                    List.of(
                            new EmpireGroundTruth("Containment Byzantine Empire", 540, 41.0, 28.9, 38.0, 4.4, 3800000.0),
                            new EmpireGroundTruth("Sasanian Empire", 540, 33.3, 44.4, 22.0, 4.1, 3600000.0)
                    ),
                    false
            ),

            // 3. 1347 CE Black Death Pandemic (1340 -> 1400 CE)
            new CalibrationScenarioDefinition(
                    "BLACK_DEATH_1347CE_UNFORCED",
                    "1347 CE Black Death Twin A: Unforced High-Density Eurasian Network Baseline (1340 -> 1400 CE)",
                    1340, 1400,
                    List.of(1340, 1347, 1353, 1370, 1390, 1400),
                    "Simulates late-medieval Eurasian agrarian density and Mongol trade routes without the introduction of the Yersinia pestis Black Death strain.",
                    375.0, 300.0,
                    4.4f, 2600.0f, 1.05f,
                    List.of(
                            new EmpireGroundTruth("Kingdom of France / Valois", 1345, 48.8, 2.3, 19.0, 4.8, 450000.0),
                            new EmpireGroundTruth("Yuan / Early Ming China", 1345, 39.9, 116.4, 85.0, 5.2, 3800000.0),
                            new EmpireGroundTruth("Mamluk Sultanate (Cairo)", 1345, 30.0, 31.2, 9.0, 4.6, 1200000.0)
                    ),
                    false
            ),
            new CalibrationScenarioDefinition(
                    "BLACK_DEATH_1347CE_COUNTERFACTUAL",
                    "1347 CE Black Death Twin B: Counterfactual Malthusian Wage Explosion & Labor Realignment (1340 -> 1400 CE)",
                    1340, 1400,
                    List.of(1340, 1347, 1353, 1370, 1390, 1400),
                    "Tests Post-Malthusian labor scarcity theory: verifies if demographic collapse triggers real wage doubling and technological capital substitution.",
                    375.0, 310.0,
                    4.4f, 2600.0f, 1.15f,
                    List.of(
                            new EmpireGroundTruth("Post-Plague High-Wage England/Flanders", 1370, 51.5, -0.1, 3.2, 5.2, 130000.0),
                            new EmpireGroundTruth("Ming Reconstruction China", 1370, 32.0, 118.8, 65.0, 5.4, 4000000.0)
                    ),
                    false
            ),

            // 4. 1492 CE Columbian Exchange & American Depopulation (1490 -> 1600 CE)
            new CalibrationScenarioDefinition(
                    "COLUMBIAN_EXCHANGE_1492CE_UNFORCED",
                    "1492 CE Columbian Exchange Twin A: Unforced Isolated Americas Baseline (1490 -> 1600 CE)",
                    1490, 1600,
                    List.of(1490, 1500, 1520, 1540, 1570, 1600),
                    "Simulates Triple Alliance (Aztec) and Tawantinsuyu (Inca) civilizational trajectories in the absence of transatlantic European contact and virgin-soil epidemics.",
                    460.0, 520.0,
                    4.8f, 3200.0f, 1.10f,
                    List.of(
                            new EmpireGroundTruth("Aztec Empire (Tenochtitlan)", 1500, 19.4, -99.1, 21.0, 4.6, 220000.0),
                            new EmpireGroundTruth("Inca Empire (Tawantinsuyu)", 1500, -13.5, -71.9, 12.0, 4.5, 2000000.0),
                            new EmpireGroundTruth("Ming Dynasty China", 1500, 39.9, 116.4, 110.0, 5.5, 4200000.0)
                    ),
                    false
            ),
            new CalibrationScenarioDefinition(
                    "COLUMBIAN_EXCHANGE_1492CE_COUNTERFACTUAL",
                    "1492 CE Columbian Exchange Twin B: Empirical Smallpox/Measles Shock & Cereal Globalization (1490 -> 1600 CE)",
                    1490, 1600,
                    List.of(1490, 1500, 1520, 1540, 1570, 1600),
                    "Falsifies epidemiological lethality functions against Seshat archaeological depopulation curves (-85% American population collapse).",
                    460.0, 540.0,
                    4.8f, 3200.0f, 1.25f,
                    List.of(
                            new EmpireGroundTruth("Spanish Viceroyalty of New Spain", 1570, 19.4, -99.1, 3.5, 5.0, 1500000.0),
                            new EmpireGroundTruth("Spanish Viceroyalty of Peru", 1570, -12.0, -77.0, 2.1, 4.9, 2500000.0),
                            new EmpireGroundTruth("Qing Precursor / Late Ming China", 1570, 39.9, 116.4, 140.0, 5.8, 4500000.0)
                    ),
                    false
            ),

            // 5. 1639 CE Sakoku Edict Isolation vs European Commercial Imperialism (1630 -> 1720 CE)
            new CalibrationScenarioDefinition(
                    "TOKUGAWA_SAKOKU_1639CE_UNFORCED",
                    "1639 CE Sakoku Japan Twin A: Autarkic Steady-State Regime Baseline (1630 -> 1720 CE)",
                    1630, 1720,
                    List.of(1630, 1650, 1670, 1690, 1720),
                    "Evaluates whether a high-density agrarian society can achieve zero-growth environmental and demographic steady-state without collapse under closed borders.",
                    540.0, 610.0,
                    5.3f, 4800.0f, 1.22f,
                    List.of(
                            new EmpireGroundTruth("Tokugawa Shogunate (Japan)", 1700, 35.7, 139.7, 30.0, 5.9, 377000.0),
                            new EmpireGroundTruth("Qing Empire (China)", 1700, 39.9, 116.4, 160.0, 6.0, 10500000.0),
                            new EmpireGroundTruth("Dutch Republic / VOC", 1700, 52.3, 4.9, 2.0, 6.8, 35000.0)
                    ),
                    false
            ),
            new CalibrationScenarioDefinition(
                    "TOKUGAWA_SAKOKU_1639CE_COUNTERFACTUAL",
                    "1639 CE Sakoku Japan Twin B: Counterfactual Open-Port Maritime Mercantile Expansion (1630 -> 1720 CE)",
                    1630, 1720,
                    List.of(1630, 1650, 1670, 1690, 1720),
                    "Tests counterfactual scenario where Japan pursues Southeast Asian maritime colonization (Red Seal ships) and early coal mining.",
                    540.0, 635.0,
                    5.3f, 4800.0f, 1.35f,
                    List.of(
                            new EmpireGroundTruth("Mercantile Japan Maritime Sphere", 1700, 35.7, 139.7, 36.0, 6.4, 600000.0),
                            new EmpireGroundTruth("Qing Empire (China)", 1700, 39.9, 116.4, 160.0, 6.0, 10500000.0)
                    ),
                    false
            )
    );

    /**
     * Archaeological Detective Scenarios for Unrecorded Anomaly Detection (Omega_dot >= 0.015 / yr).
     */
    public static final List<CalibrationScenarioDefinition> ARCHAEOLOGICAL_DETECTIVE_SCENARIOS = List.of(
            // 1. Harappa / Indus Valley Civilization Collapse (-1900 -> -1500 BCE)
            new CalibrationScenarioDefinition(
                    "HARAPPA_INDUS_COLLAPSE_1900BCE",
                    "Archaeological Detective: Indus Valley / Harappa Urban De-densification (-1900 -> -1500 BCE)",
                    -1900, -1500,
                    List.of(-1900, -1800, -1700, -1600, -1500),
                    "Investigating the desiccation of the Ghaggar-Hakra river system and monsoon migration driving urban de-densification without foreign invasion.",
                    35.0, 32.0,
                    2.6f, 950.0f, 0.72f,
                    List.of(
                            new EmpireGroundTruth("Mature Harappan Urban Hubs", -1900, 27.5, 68.1, 5.0, 3.2, 800000.0),
                            new EmpireGroundTruth("Post-Urban Rural Dispersal", -1500, 24.0, 75.0, 4.2, 2.8, 500000.0)
                    ),
                    false
            ),

            // 2. Roman Crisis of the Third Century (235 -> 284 CE)
            new CalibrationScenarioDefinition(
                    "ROMAN_THIRD_CENTURY_CRISIS_235CE",
                    "Archaeological Detective: Roman Empire Third-Century Anarchy & Plague of Cyprian (235 -> 284 CE)",
                    235, 284,
                    List.of(235, 250, 260, 270, 284),
                    "Investigating multi-stressor convergence (Cyprian epidemic, hyper-inflationary denarius silver debasement, and Germanic incursions) before Diocletian tetrarchy.",
                    190.0, 175.0,
                    3.7f, 1500.0f, 0.82f,
                    List.of(
                            new EmpireGroundTruth("Fragmented Gallic Empire", 260, 48.0, 2.0, 12.0, 3.8, 600000.0),
                            new EmpireGroundTruth("Palmyrene Empire", 260, 34.5, 38.2, 9.0, 3.9, 800000.0),
                            new EmpireGroundTruth("Central Roman Administration", 270, 41.9, 12.5, 30.0, 3.8, 2200000.0)
                    ),
                    false
            ),

            // 3. Classic Maya Lowlands Collapse (800 -> 950 CE)
            new CalibrationScenarioDefinition(
                    "CLASSIC_MAYA_COLLAPSE_800CE",
                    "Archaeological Detective: Classic Maya Lowlands Karst Mega-Drought & Palace Abandonment (800 -> 950 CE)",
                    800, 950,
                    List.of(800, 830, 860, 900, 950),
                    "Investigating centennial drought pulses in the Yucatan karst basin coupled with elite warfare and soil erosion causing 80% urban depopulation.",
                    220.0, 235.0,
                    3.9f, 1600.0f, 0.85f,
                    List.of(
                            new EmpireGroundTruth("Southern Lowland Maya (Tikal/Calakmul)", 800, 17.2, -89.6, 7.5, 4.2, 120000.0),
                            new EmpireGroundTruth("Northern Highland Migration (Chichen Itza)", 950, 20.6, -88.5, 3.0, 4.0, 80000.0)
                    ),
                    false
            )
    );

    /**
     * Master Multi-Millennial 9-Epoch Historical Slices (-100,000 BP to 2026 CE).
     * Pure unforced physics within each epoch window to evaluate intrinsic drift without artificial nudging.
     */
    public static final List<CalibrationScenarioDefinition> MASTER_NINE_EPOCH_BLOCKS = List.of(
            new CalibrationScenarioDefinition(
                    "EPOCH_1_PALEOLITHIC_DISPERSAL",
                    "Epoch 1: Paleolithic Out-of-Africa Dispersal (-100000 -> -50000 BP)",
                    -100000, -50000,
                    List.of(-100000, -74000, -60000, -50000),
                    "Early coastal dispersal of Homo sapiens along Indian Ocean rim, Toba cataclysm, and megafauna hunting dynamics.",
                    0.20, 1.5,
                    1.0f, 150.0f, 0.30f,
                    List.of(new EmpireGroundTruth("African Core Population", -70000, 5.0, 35.0, 0.05, 1.2, 15000000.0)),
                    true
            ),
            new CalibrationScenarioDefinition(
                    "EPOCH_2_UPPER_PALEOLITHIC_LGM",
                    "Epoch 2: Upper Paleolithic & Last Glacial Maximum (-50000 -> -10000 BP)",
                    -50000, -10000,
                    List.of(-50000, -30000, -20000, -10000),
                    "Eurasian colonization, Sahul maritime settlement, Beringia standstill, and LGM thermal refugia.",
                    1.5, 4.5,
                    1.5f, 250.0f, 0.40f,
                    List.of(new EmpireGroundTruth("Franco-Cantabrian Refugium", -20000, 43.5, -2.0, 0.08, 1.8, 300000.0)),
                    true
            ),
            new CalibrationScenarioDefinition(
                    "EPOCH_3_NEOLITHIC_FERTILE_CRESCENT",
                    "Epoch 3: Neolithic Revolution & Agrarian Sedentism (-10000 -> -3000 BCE)",
                    -10000, -3000,
                    List.of(-10000, -8000, -6000, -4000, -3000),
                    "Holocene thermal optimum, Natufian grain storage, cereal domestication in Fertile Crescent, and Green Sahara wet phase.",
                    4.5, 45.0,
                    2.2f, 600.0f, 0.60f,
                    List.of(new EmpireGroundTruth("Fertile Crescent Agrarian Clusters", -5000, 36.0, 40.0, 3.5, 2.5, 800000.0)),
                    true
            ),
            new CalibrationScenarioDefinition(
                    "EPOCH_4_BRONZE_AGE_URBANIZATION",
                    "Epoch 4: Bronze Age Metallurgy & Early Hydraulic States (-3000 -> -500 BCE)",
                    -3000, -500,
                    List.of(-3000, -2000, -1200, -800, -500),
                    "Nile flood basin unification, Sumerian cuneiform, Harappan urbanization, and 1200 BCE palatial collapse.",
                    45.0, 100.0,
                    2.8f, 1000.0f, 0.78f,
                    List.of(new EmpireGroundTruth("Old / New Kingdom Egypt", -2000, 26.0, 32.0, 3.5, 3.1, 500000.0)),
                    true
            ),
            new CalibrationScenarioDefinition(
                    "EPOCH_5_CLASSICAL_ANTIQUITY",
                    "Epoch 5: Classical Axial Antiquity & Continental Empires (-500 BCE -> 500 CE)",
                    -500, 500,
                    List.of(-500, -334, 0, 250, 500),
                    "Pax Romana maritime trade, Han Dynasty silk routes, Maurya India, and Antonine/Cyprian epidemic shocks.",
                    100.0, 205.0,
                    3.6f, 1600.0f, 0.88f,
                    List.of(new EmpireGroundTruth("Roman Empire", 0, 41.9, 12.5, 54.0, 3.8, 3800000.0)),
                    true
            ),
            new CalibrationScenarioDefinition(
                    "EPOCH_6_EARLY_MEDIEVAL_ISLAMIC",
                    "Epoch 6: Late Antiquity & Early Islamic Expansion (500 -> 1000 CE)",
                    500, 1000,
                    List.of(500, 536, 632, 750, 1000),
                    "536 CE volcanic cooling, Justinian plague, Umayyad/Abbasid caliphate, and Tang Dynasty urban expansion.",
                    205.0, 265.0,
                    3.9f, 1900.0f, 0.90f,
                    List.of(new EmpireGroundTruth("Abbasid Caliphate", 800, 33.3, 44.4, 28.0, 4.4, 6500000.0)),
                    true
            ),
            new CalibrationScenarioDefinition(
                    "EPOCH_7_HIGH_LATE_MEDIEVAL",
                    "Epoch 7: High Medieval & Eurasian Nomad Dynamics (1000 -> 1500 CE)",
                    1000, 1500,
                    List.of(1000, 1100, 1206, 1347, 1500),
                    "Song hydraulic mechanization, Gengis Khan Mongol conquests, Qanat destructuring, and 1347 Black Death pandemic.",
                    265.0, 425.0,
                    4.4f, 2500.0f, 1.05f,
                    List.of(new EmpireGroundTruth("Song Dynasty China", 1100, 34.8, 114.3, 100.0, 5.0, 3100000.0)),
                    true
            ),
            new CalibrationScenarioDefinition(
                    "EPOCH_8_EARLY_MODERN_GLOBAL",
                    "Epoch 8: Early Modern Columbian Exchange & Commercial Networks (1500 -> 1850 CE)",
                    1500, 1850,
                    List.of(1500, 1600, 1700, 1750, 1800, 1850),
                    "Columbian contact, American virgin-soil epidemics, Ming/Qing transition, and pre-industrial coal emergence.",
                    425.0, 1260.0,
                    5.8f, 12000.0f, 1.55f,
                    List.of(new EmpireGroundTruth("Qing Dynasty", 1800, 39.9, 116.4, 300.0, 6.8, 13000000.0)),
                    true
            ),
            new CalibrationScenarioDefinition(
                    "EPOCH_9_INDUSTRIAL_ANTHROPOCENE",
                    "Epoch 9: Industrial Revolution & The Great Acceleration (1850 -> 2026 CE)",
                    1850, 2026,
                    List.of(1850, 1900, 1914, 1950, 1990, 2026),
                    "Coal/steam/oil thermodynamic transition, Haber-Bosch nitrogen, total war disruptions, and globalized exergy expansion.",
                    1260.0, 8050.0,
                    8.5f, 140000.0f, 3.40f,
                    List.of(new EmpireGroundTruth("United States & Global Network", 2000, 38.9, -77.0, 282.0, 10.5, 9800000.0)),
                    true
            )
    );
}
