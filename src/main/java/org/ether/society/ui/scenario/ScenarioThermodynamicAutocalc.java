/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

/**
 * Historical thermodynamics, metabolic scaling, and per-capita civilizational capacity
 * analytical estimators.
 *
 * <p>Provides $O(1)$ analytical scaling functions mapping historical chronological years
 * ($t \in [-100\,000, +2\,100]$) to baseline socioeconomic stocks per capita:
 * <ul>
 *   <li>Capital stock per capita: $K_0(t)$ [$\text{kg/hab}$ or $\text{1990 Int. \$ per capita}$]</li>
 *   <li>Exergy / energy throughput per capita: $E_0(t)$ [$\text{MJ/hab/year}$]</li>
 *   <li>Food security reserves: $F_0(t)$ [$\text{months of autarkic caloric supply}$]</li>
 *   <li>Information density: $I_0(t)$ [$\text{bits/hab}$]</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class ScenarioThermodynamicAutocalc {

    private ScenarioThermodynamicAutocalc() {
        // Utility class
    }

    /**
     * Computes the canonical physical capital stock per capita $K_0$ as a function of start year.
     *
     * <p>Empirical scaling derived from archaeological material culture estimates and Maddison Project:
     * \begin{equation}
     * K_0(t) = \begin{cases}
     * 1.0\,\text{kg/hab} & \text{if } t \le -50\,000 \\
     * 5.0\,\text{kg/hab} & \text{if } -50\,000 < t \le -10\,000 \\
     * 25.0\,\text{kg/hab} & \text{if } -10\,000 < t \le -3\,000 \\
     * 100.0\,\text{kg/hab} & \text{if } -3\,000 < t \le -500 \\
     * 200.0\,\text{kg/hab} & \text{if } -500 < t \le 1\,000 \\
     * 500.0\,\text{kg/hab} & \text{if } 1\,000 < t \le 1\,500 \\
     * 2\,500.0\,\text{kg/hab} & \text{if } 1\,500 < t \le 1\,800 \\
     * 8\,000.0\,\text{kg/hab} & \text{if } 1\,800 < t \le 1\,950 \\
     * 15\,000.0\,\text{kg/hab} & \text{if } t > 1\,950
     * \end{cases}
     * \end{equation}
     *
     * @param year the simulation start year $t$ in years CE/BCE
     * @return capital stock $K_0$ in kg/capita equivalent
     */
    public static double computeAutoCapitalFromYear(long year) {
        if (year <= -50000) return 1.0;    // Early Paleolithic / Out of Africa (~1 kg/hab)
        if (year <= -10000) return 5.0;    // Late Paleolithic / Early Neolithic (~5 kg/hab)
        if (year <= -3000)  return 25.0;   // Bronze Age (~25 kg/hab)
        if (year <= -500)   return 100.0;  // Antiquity (~100 kg/hab)
        if (year <= 1000)   return 200.0;  // Early Middle Ages (~200 kg/hab)
        if (year <= 1500)   return 500.0;  // Renaissance / Age of Discovery (~500 kg/hab)
        if (year <= 1800)   return 2500.0; // Industrial Revolution (~2500 kg/hab)
        if (year <= 1950)   return 8000.0; // 20th Century (~8000 kg/hab)
        return 15000.0;                    // Modern / Post-Industrial (>15000 kg/hab)
    }

    /**
     * Computes the baseline exergy and energy throughput per capita $E_0$ as a function of start year.
     *
     * <p>Exergy values based on Vaclav Smil's historical energy transition data:
     * \begin{equation}
     * E_0(t) \in [10.0, 50\,000.0]\,\text{MJ/hab}
     * \end{equation}
     *
     * @param year the simulation start year $t$ in years CE/BCE
     * @return energy throughput $E_0$ in MJ/capita
     */
    public static double computeAutoEnergyFromYear(long year) {
        if (year <= -10000) return 10.0;    // Firewood (~10 MJ/hab)
        if (year <= -3000)  return 30.0;    // Wood & fodder (~30 MJ/hab)
        if (year <= -500)   return 60.0;    // Oil & firewood (~60 MJ/hab)
        if (year <= 1000)   return 150.0;   // Hydraulic & firewood (~150 MJ/hab)
        if (year <= 1500)   return 300.0;   // Peat & timber (~300 MJ/hab)
        if (year <= 1800)   return 2500.0;  // Coal mines (~2500 MJ/hab)
        if (year <= 1950)   return 10000.0; // Hydrocarbons (~10000 MJ/hab)
        return 50000.0;                     // Modern/Future (>50000 MJ/hab)
    }

    /**
     * Computes the autarkic food reserve duration $F_0$ in months as a function of start year.
     *
     * @param year the simulation start year $t$ in years CE/BCE
     * @return food reserves in months of consumption
     */
    public static double computeAutoFoodFromYear(long year) {
        if (year <= -10000) return 2.0;  // Foraging (2 months)
        if (year <= -3000)  return 4.0;  // Early granaries (4 months)
        if (year <= -500)   return 6.0;  // Roman/Han silos (6 months)
        if (year <= 1000)   return 6.0;  // Feudal granaries (6 months)
        if (year <= 1500)   return 8.0;  // Early modern storage (8 months)
        if (year <= 1800)   return 10.0; // Agricultural revolution (10 months)
        if (year <= 1950)   return 12.0; // Modern global logistics (12 months)
        return 18.0;                     // Future/Post-scarcity (18+ months)
    }

    /**
     * Computes the active cultural/technological information density $I_0$ as a function of start year.
     *
     * @param year the simulation start year $t$ in years CE/BCE
     * @return information stock in bits/capita
     */
    public static double computeAutoInfoFromYear(long year) {
        if (year <= -10000) return 5.0;        // Oral tradition (~5 bits/hab)
        if (year <= -3000)  return 40.0;       // Cuneiform/Hieroglyphs (~40 bits/hab)
        if (year <= -500)   return 200.0;      // Classical manuscripts (~200 bits/hab)
        if (year <= 1000)   return 500.0;      // Scriptoriums & block printing (~500 bits/hab)
        if (year <= 1500)   return 2000.0;     // Printing press (~2000 bits/hab)
        if (year <= 1800)   return 15000.0;    // Encyclopedias & press (~15000 bits/hab)
        if (year <= 1950)   return 500000.0;   // Telecommunications & radio (~500000 bits/hab)
        return 5000000.0;                      // Internet & AI (>5000000 bits/hab)
    }
}
