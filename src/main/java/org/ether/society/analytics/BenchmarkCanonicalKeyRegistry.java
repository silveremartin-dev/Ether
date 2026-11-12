/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Canonical 27-variable benchmark keys registry for comparative analytics.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class BenchmarkCanonicalKeyRegistry {

    private static final Map<String, String> CANONICAL_KEYS = initCanonicalKeys();

    private BenchmarkCanonicalKeyRegistry() {
        // Utility catalog
    }

    private static Map<String, String> initCanonicalKeys() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("👥 Global Demographic Population (worldPopulation)", "worldPopulation");
        m.put("💰 Gross World Product (grossWorldProduct)", "grossWorldProduct");
        m.put("⚡ Primary Energy Consumption (primaryEnergy)", "primaryEnergy");
        m.put("🏙️ Urbanization Rate (urbanizationRate)", "urbanizationRate");
        m.put("🌿 Atmospheric CO2 Concentration (co2Concentration)", "co2Concentration");
        m.put("📖 Global Literacy Rate (literacyRate)", "literacyRate");
        m.put("🪙 Currency Debasement / Coinage Purity (currencyDebasement)", "currencyDebasement");
        m.put("👑 Elite Overproduction Index (eliteOverproductionIndex)", "eliteOverproductionIndex");
        m.put("🍞 Real Unskilled Wage (realUnskilledWage)", "realUnskilledWage");
        m.put("🏛️ Political Stress Index (politicalStressIndex)", "politicalStressIndex");
        m.put("🤝 Asabiyyah Social Cohesion (asabiyyahSocialCohesion)", "asabiyyahSocialCohesion");
        m.put("📉 Sociopolitical Instability (sociopoliticalInstability)", "sociopoliticalInstability");
        m.put("⚖️ Gini Inequality (giniInequality)", "giniInequality");
        m.put("📜 Sovereign Debt Burden (sovereignDebtBurden)", "sovereignDebtBurden");
        m.put("🚢 Global Trade Volume (globalTradeVolume)", "globalTradeVolume");
        m.put("🌡️ Temperature Anomaly (temperatureAnomaly)", "temperatureAnomaly");
        m.put("🌱 Topsoil Erosion Loss (soilErosionRate)", "soilErosionRate");
        m.put("🌾 Agricultural EROEI (agriculturalEroei)", "agriculturalEroei");
        m.put("🌲 Forest Cover Remaining (deforestationRate)", "deforestationRate");
        m.put("⚡ Information Travel Speed (informationSpeed)", "informationSpeed");
        m.put("🦣 Megafauna Abundance Index (megafaunaIndex)", "megafaunaIndex");
        m.put("☀️ Milankovitch 65°N Insolation (milankovitchInsolation)", "milankovitchInsolation");
        m.put("🛡️ Biogeographical Zero-Containment (zeroContainmentScore)", "zeroContainmentScore");
        m.put("👶 Life Expectancy at Birth (lifeExpectancyAtBirth)", "lifeExpectancyAtBirth");
        m.put("⚒️ Metal Smelting Per Capita (metalSmeltingPerCapita)", "metalSmeltingPerCapita");
        m.put("⛏️ Fossil Share Primary Energy (fossilSharePrimaryEnergy)", "fossilSharePrimaryEnergy");
        m.put("⚓ Shipping Freight Cost Index (shippingFreightCostIndex)", "shippingFreightCostIndex");
        return Collections.unmodifiableMap(m);
    }

    /**
     * Builds or retrieves the ordered map of display labels to canonical 27-variable benchmark keys.
     *
     * @return unmodifiable map of display labels to technical metric keys
     */
    public static Map<String, String> getCanonicalBenchmarkKeyMap() {
        return CANONICAL_KEYS;
    }
}
