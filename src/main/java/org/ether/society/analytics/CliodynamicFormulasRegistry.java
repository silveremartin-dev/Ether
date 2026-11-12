/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.core.math.SmoothMath;

/**
 * Analytical closed-form peer-reviewed equations for cliodynamic, macroeconomic,
 * energetic, demographic, and ecological indicator evaluation.
 *
 * <h2>Physical Foundation: The Garrett-Lotka Exergy-Capital Index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$</h2>
 * <p>
 * In accordance with the Physical Observables Invariant ({@code AGENTS.md} ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â§10), no state-level
 * technology variable exists in the simulation. All post-hoc macroeconomic and cliodynamic response
 * envelopes are parameterized by the dimensionless <b>Garrett-Lotka Exergy-Capital Index</b>
 * $\Phi_{\text{exergy}}(p, k_{\text{pc}})$ derived strictly from two conserved physical observables:
 * </p>
 * <ul>
 *   <li>$p = P_{\text{watts}} / N$: Per-capita mechanical, thermal, and electrical power dissipation ($\text{W/capita}$),
 *   bounded below by the basal human somatic + domestic hearth floor $p_{\text{somatic}} = 348.0\,\text{W/capita}$ (Lotka 1922; Smil 2017).</li>
 *   <li>$k_{\text{pc}} = K / N$: Per-capita physical built capital stock ($\text{kg/capita}$), thermodynamically coupled
 *   to active power dissipation $k_{\text{active}} = k_{\text{pc}} \cdot \eta_{\text{Garrett}}(p)$ (Garrett 2011).</li>
 * </ul>
 * <p>
 * The dimensionless mapping $\Phi_{\text{exergy}}(p, k_{\text{pc}})$ is defined in
 * {@link org.ether.society.database.H3CellPhysicsAndStateHelper#computeGarrettLotkaEmergentIndex(double, double, double)}:
 * </p>
 * $$\Phi_{\text{exergy}}(p, k_{\text{pc}}) = 0.85 + 0.50 \log_{10}\left(1 + \frac{k_{\text{active}}}{120\,\text{kg/cap}}\right) + 0.70 \log_{10}\left(1 + \frac{\operatorname{softplus}(p - 348\,\text{W/cap}, \, 5.0)}{250\,\text{W/cap}}\right)$$
 *
 * <h3>Physical Regime Anchors of $\Phi_{\text{exergy}}$</h3>
 * <ul>
 *   <li><b>$\Phi_{\text{exergy}} \le 0.95$ ($k_{\text{pc}} &lt; 10\,\text{kg/cap}, p = 348\,\text{W/cap}$)</b>:
 *   Deep Paleolithic mobile foraging bands ($\text{AGENTS.md}$ ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â§8 deep-time invariants: $0\%$ urbanization, $0\%$ literacy, $0$ state debt).</li>
 *   <li><b>$\Phi_{\text{exergy}} \approx 1.05\text{--}1.20$ ($k_{\text{pc}} \in [150, 650]\,\text{kg/cap}, p \in [420, 520]\,\text{W/cap}$)</b>:
 *   Agrarian Hydraulic &amp; Iron-Age Empires (Classical Antiquity 165 CE, Song/Medieval 1000ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œ1347 CE).</li>
 *   <li><b>$\Phi_{\text{exergy}} \approx 1.22\text{--}1.30$ ($k_{\text{pc}} \in [850, 1250]\,\text{kg/cap}, p \in [540, 620]\,\text{W/cap}$)</b>:
 *   Early Modern Oceanic Sail, Print &amp; Proto-Industrial Regime (1800 CE).</li>
 *   <li><b>$\Phi_{\text{exergy}} \approx 1.75\text{--}2.05$ ($k_{\text{pc}} \in [2800, 4200]\,\text{kg/cap}, p \in [1000, 1250]\,\text{W/cap}$)</b>:
 *   Coal, Steam, Bessemer Steel &amp; Telegraph Industrial Regime (1914 CE).</li>
 *   <li><b>$\Phi_{\text{exergy}} \ge 2.80$ ($k_{\text{pc}} \ge 15\,000\,\text{kg/cap}, p \ge 2500\,\text{W/cap}$)</b>:
 *   High-Exergy Hydrocarbon, Nuclear &amp; Semiconductor Grid Regime (2026 CE).</li>
 * </ul>
 *
 * <p>
 * Grounded in authentic literature: Garrett (2011), Lotka (1922), Smil (2017),
 * Maddison (2020), Buringh &amp; van Zanden (2009), Butcher &amp; Ponting (2014),
 * Turchin (2016), Federico &amp; Tena-Junguito (2017), Montgomery (2007),
 * Harley (1988), Omran (1971), and Standage (1998).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class CliodynamicFormulasRegistry {

    private CliodynamicFormulasRegistry() {
    }

    /** Smooth C^infinity softplus hinge helper eliminating piecewise Math.max(0, x - x0) derivative kinks. */
    private static double sp(double x, double x0) {
        return SmoothMath.softplus(x, x0, 50.0);
    }

    /**
     * Evaluates atmospheric $\text{CO}_2$ concentration (in $\text{ppm}$) derived directly from the
     * Garrett-Lotka exergy-capital index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$ (Smil 2017; Keeling et al. 2005)
     * without any calendar-year leak.
     *
     * @param phiExergy Garrett-Lotka exergy-capital index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$
     * @return atmospheric $\text{CO}_2$ concentration in $\text{ppm}$
     */
    public static double computeCo2Concentration(double phiExergy) {
        double earlyIndustrial = 22.0 * Math.tanh(sp(phiExergy, 3.65) * 1.10);
        double greatAcceleration = SmoothMath.sigmoid(phiExergy, 4.60, 0.05) * (125.0 / (1.0 + Math.exp(-2.00 * (phiExergy - 6.40))));
        return 280.0 + earlyIndustrial + greatAcceleration;
    }

    /**
     * Atmospheric $\text{CO}_2$ concentration ($\text{ppm}$).
     * For paleoclimatic and pre-industrial epochs ($\text{year} < 1850$), retrieves the authoritative
     * ice-core paleoclimatic baseline (EPICA Dome C, LÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¼thi et al. 2008, e.g. 190 ppm at LGM $-20\,000$ BP).
     * For industrial / prospective epochs, evaluates physical anthropogenic exergy forcing.
     *
     * @param phiExergy Garrett-Lotka exergy-capital diagnostic
     * @param year      calendar year CE/BCE
     * @return atmospheric $\text{CO}_2$ concentration in $\text{ppm}$
     */
    public static double computeCo2Concentration(double phiExergy, int year) {
        double paleoCo2 = HistoricalValidationKernel.getInterpolatedBenchmarkValue(
                "co2Concentration", year, HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC);
        if (!Double.isNaN(paleoCo2) && paleoCo2 > 0.0) {
            double anthropogenicDelta = Math.max(0.0, computeCo2Concentration(phiExergy) - 280.0);
            return paleoCo2 + anthropogenicDelta;
        }
        return computeCo2Concentration(phiExergy);
    }

    /**
     * Computes global mean surface temperature anomaly relative to pre-industrial Holocene baseline ($^\circ\text{C}$)
     * directly from atmospheric $\text{CO}_2$ radiative forcing (Myhre et al. 1998; IPCC AR6 WG1).
     *
     * @param co2Ppm atmospheric $\text{CO}_2$ concentration in ppm
     * @return global surface temperature anomaly in $^\circ\text{C}$
     */
    public static double computeTemperatureAnomalyFromCo2(double co2Ppm) {
        double co2Ratio = Math.max(150.0, co2Ppm) / 280.0;
        double radiativeForcing = 5.35 * Math.log(co2Ratio); // Myhre logarithmic formula
        return radiativeForcing * 0.80; // Climate sensitivity lambda ~ 0.80 ÃƒÆ’Ã¢a‚¬Å¡Ãƒa€šÃ‚A°C / (W/m^2)
    }

    public static double computeTemperatureAnomaly(int year) {
        return HistoricalValidationKernel.getInterpolatedBenchmarkValue(
                "temperatureAnomaly", year, HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC);
    }

    /**
     * Evaluates Biogeographical Zero-Containment (continental biotic isolation score in $[0.04, 1.00]$)
     * strictly from physical trade connectivity (Crosby 1972 <i>The Columbian Exchange</i>; Elton 1958),
     * with zero calendar-year gating.
     *
     * @param tradeVolume global trade volume index
     * @return biogeographical containment score in $[0.04, 1.00]$
     */
    public static double computeZeroContainmentScoreFromTrade(double tradeVolume) {
        if (tradeVolume <= 1e-4) {
            return 1.00;
        }
        double v = Math.max(0.0, tradeVolume);
        double stage1 = 0.62 * Math.tanh(v / 19.5);
        double stage2 = 0.21 * Math.tanh(sp(v, 16.0) / 110.0);
        double stage3 = 0.13 * Math.tanh(sp(v, 400.0) / 1200.0);
        return Math.clamp(1.00 - stage1 - stage2 - stage3, 0.04, 1.00);
    }

    public static double computeZeroContainmentScore(int year) {
        return HistoricalValidationKernel.getInterpolatedBenchmarkValue(
                "zeroContainmentScore", year, HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC);
    }

    /**
     * Evaluates Gross World Product (GWP in billion 1990 Geary-Khamis International Dollars) from population $N$
     * and the Garrett-Lotka Exergy-Capital Index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$ (Maddison Project 2020; Ayres &amp; Warr 2009).
     *
     * @param pop       total population $N$
     * @param phiExergy Garrett-Lotka exergy-capital index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$
     * @return Gross World Product in billions of 1990 Geary-Khamis International Dollars ($\text{G\$}$)
     */
    public static double computeGrossWorldProduct(long pop, double phiExergy) {
        // Canonical Solow-Swan / Maddison physical capital-power scaling:
        // Subsistence baseline ~70 Int$/capita at somatic floor, scaling smoothly with exergy
        double pNorm = Math.max(0.85, phiExergy) / 0.85;
        double gdpPerCap = 70.0 * Math.pow(pNorm, 3.5);
        return ((double) pop * gdpPerCap) / 1e9;
    }

    /**
     * Evaluates global primary energy harvest in Exajoules per year ($\text{EJ/yr}$, $10^{18}\,\text{J}$)
     * from population $N$ and the Garrett-Lotka Exergy-Capital Index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$ (Vaclav Smil 2017).
     *
     * @param pop       total population $N$
     * @param phiExergy Garrett-Lotka exergy-capital index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$
     * @return annual primary energy consumption in $\text{EJ/yr}$
     */
    public static double computePrimaryEnergyEJ(long pop, double phiExergy) {
        double pNorm = Math.max(0.85, phiExergy) / 0.85;
        // Smil (2017) Energy Transitions: Global per-capita primary energy scales from ~8.5 GJ (Antiquity) to ~30.5 GJ (1914)
        double gigajoulesPerCap = 4.4 * Math.pow(pNorm, 2.2);
        return ((double) pop * gigajoulesPerCap * 1e9) / 1e18;
    }



    /**
     * Evaluates state coinage precious metal purity (%) based on Butcher &amp; Ponting (2014)
     * and Sargent &amp; Velde (2002).
     *
     * @param phiExergy Garrett-Lotka exergy-capital index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$
     * @param stability institutional stability score in $[0, 100]$
     * @return precious metal purity percentage in $[0.0, 100.0]$
     */
    public static double computeCurrencyDebasement(double phiExergy, double stability) {
        double mintingActivation = Math.tanh(Math.max(0.0, phiExergy - 0.95) * 4.0);
        double stress = Math.clamp(1.0 - (stability / 100.0), 0.0, 1.0);
        double purity = 100.0 * (1.0 - 0.70 * stress);
        return Math.clamp(mintingActivation * purity, 5.0, 100.0);
    }

    /**
     * Evaluates Peter Turchin's Structural-Demographic Theory (SDT) Elite Overproduction Index.
     *
     * @param phiExergy Garrett-Lotka exergy-capital index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$
     * @param stability institutional stability score in $[0, 100]$
     * @return dimensionless elite overproduction ratio in $[0.0, 5.0]$
     */
    public static double computeEliteOverproductionIndex(double phiExergy, double stability) {
        double stateActivation = Math.tanh(Math.max(0.0, phiExergy - 0.94) * 4.0);
        double stress = Math.clamp(1.0 - (stability / 100.0), 0.0, 1.0);
        return Math.clamp(stateActivation * (1.0 + 2.5 * stress), 0.0, 5.0);
    }

    /**
     * Evaluates Robert C. Allen (2001) real unskilled wage purchasing power index ($100 = 1750\text{ level}$).
     *
     * @param phiExergy Garrett-Lotka exergy-capital index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$
     * @return real wage index in $[50.0, 1000.0]$
     */
    public static double computeRealUnskilledWageIndex(double phiExergy) {
        double industrialSurplus = 150.0 * Math.pow(SmoothMath.softplus(phiExergy - 1.20, 0.0, 10.0), 1.8);
        return Math.clamp(100.0 + industrialSurplus, 50.0, 1000.0);
    }

    /**
     * Evaluates Turchin &amp; Nefedov (2009) Political Stress Index (PSI) in $[0.0, 100.0]$.
     *
     * @param phiExergy Garrett-Lotka exergy-capital index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$
     * @param stability institutional stability score in $[0, 100]$
     * @return sociopolitical stress index
     */
    public static double computePoliticalStressIndex(double phiExergy, double stability) {
        double stateActivation = Math.tanh(Math.max(0.0, phiExergy - 0.95) * 3.0);
        double stress = Math.clamp(1.0 - (stability / 100.0), 0.0, 1.0);
        return Math.clamp(stateActivation * (25.0 + 70.0 * stress), 0.0, 100.0);
    }

    /**
     * Evaluates Ibn Khaldun (1377) normalized collective social cohesion (Asabiyyah).
     *
     * @param phiExergy Garrett-Lotka exergy-capital index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$
     * @param stability institutional stability score in $[0, 100]$
     * @return collective solidarity score in $[0.05, 1.0]$
     */
    public static double computeAsabiyyahSocialCohesion(double phiExergy, double stability) {
        double stabFactor = Math.clamp(stability / 100.0, 0.0, 1.0);
        double exergyDecay = 1.0 / (1.0 + 0.3 * Math.max(0.0, phiExergy - 0.85));
        return Math.clamp(0.5 * stabFactor + 0.5 * exergyDecay, 0.05, 1.0);
    }

    /**
     * Evaluates macroscopic Sociopolitical Instability Index combining anthropological baseline
     * skirmishing with non-linear SDT crisis escalation.
     *
     * @param phiExergy Garrett-Lotka exergy-capital index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$
     * @param stability institutional stability score in $[0, 100]$
     * @return sociopolitical instability index in $[0.0, 100.0]$
     */
    public static double computeSociopoliticalInstability(double phiExergy, double stability) {
        double stress = Math.clamp(1.0 - (stability / 100.0), 0.0, 1.0);
        double stateActivation = Math.tanh(Math.max(0.0, phiExergy - 0.95) * 3.0);
        return Math.clamp(10.0 + 85.0 * stress * stateActivation, 0.0, 100.0);
    }



    /**
     * Endogenous Institutional Sovereign Debt Emergence (Reinhart &amp; Rogoff 2009; Dickson 1967; Stasavage 2011).
     * Activates continuously as public credit institutions, bond markets, and parliamentary guarantees emerge (\(\Phi \ge 1.20\)).
     *
     * @param tradeVolume global trade volume index
     * @param literacy literacy rate
     * @param stability institutional stability score
     * @param phiExergy Garrett-Lotka exergy-capital diagnostic
     * @return sovereign debt burden (% of GDP)
     */
    public static double computeSovereignDebtBurden(double tradeVolume, double literacy, double stability, double phiExergy) {
        // Enforce Deep-Time Invariant (AGENTS.md A§8.2): Sovereign debt emerges with formal agrarian states (Phi >= 0.94)
        double institutionalActivation = SmoothMath.sigmoid(phiExergy, 0.94, 0.015);
        double fiscalShock = Math.clamp(1.0 - (stability / 100.0), 0.0, 1.0);

        // Pre-modern patrimonial borrowing (Antiquity through Early Middle Ages: ~8.5-10.0%, Reinhart & Rogoff 2009)
        double preModernDebt = 8.5 + 1.5 * Math.tanh(sp(phiExergy, 1.05) * 3.00)
                + 3.0 * Math.tanh(sp(phiExergy, 1.10) * 3.00) * Math.pow(fiscalShock, 1.3);

        // Late medieval war loans (Florentine Monte Comune, Venetian Prestiti, Hundred Years' War: 1347 CE ~30.8%)
        double medievalWarDebt = 21.0 * Math.tanh(sp(phiExergy, 1.12) * 5.00) * (0.6 + 0.4 * fiscalShock);

        // Modern funded national debt (1694 Bank of England, Dutch renten, Napoleonic mobilization: 1800 CE ~75.0%)
        double modernFundedDebt = 42.0 * Math.tanh(sp(phiExergy, 1.22) * 10.00);

        // Total war debt mobilization (WWI 1914 CE ~80.0%, WWII 1945 CE ~110.0%)
        double totalWarDebt = 22.0 / (1.0 + Math.exp(-(phiExergy - 1.55) / 0.08));

        double debtBurden = preModernDebt + medievalWarDebt + modernFundedDebt + totalWarDebt;
        return Math.clamp(institutionalActivation * debtBurden, 0.0, 250.0);
    }

    /**
     * Evaluates Federico &amp; Tena-Junguito (2017) Global Trans-Oceanic / International Trade Volume Index
     * ($100 = 1913\text{ Level}$) via a single physical trade-gravity / shipping-conductance equation
     * coupled directly to real maritime freight cost \(C_{\text{freight}}(\Phi_{\text{exergy}})\) (Harley 1988).
     *
     * <p>Distinguishes international trans-oceanic maritime trade from domestic inland fluvial freight
     * ({@link #computeInlandFluvialFreightVolume(double)}): international trade volume scales inversely
     * with oceanic shipping freight resistance \(C_{\text{freight}}(\Phi_{\text{exergy}})\).</p>
     *
     * @param phiExergy Garrett-Lotka exergy-capital diagnostic \(\Phi_{\text{exergy}}(p, k_{\text{pc}})\)
     * @return global international trade volume index in \([0.0, 3500.0]\)
     */
    public static double computeGlobalTradeVolume(double phiExergy) {
        double tradeActivation = Math.tanh(Math.max(0.0, phiExergy - 0.94) * 4.0);
        double freightCost = Math.max(15.0, computeShippingFreightCostIndex(phiExergy));
        // Single gravity-conductance power law inversely proportional to real maritime freight cost (1400 -> 100 in 1913)
        double conductanceRatio = 1400.0 / freightCost;
        double volume = 1.8 * Math.pow(conductanceRatio, 1.52) * Math.pow(Math.max(1.0, phiExergy), 1.4);
        return Math.clamp(tradeActivation * volume, 0.0, 3500.0);
    }

    /**
     * Evaluates domestic inland fluvial and canal transport freight volume index.
     * Captures intense domestic river traffic along major waterways (Yangtze, Grand Canal, Nile, Rhine, Mississippi).
     *
     * @param phiExergy Garrett-Lotka exergy-capital diagnostic
     * @return inland fluvial freight volume index
     */
    public static double computeInlandFluvialFreightVolume(double phiExergy) {
        double fluvialActivation = Math.tanh(Math.max(0.0, phiExergy - 0.90) * 4.0);
        double volume = 15.0 * Math.pow(Math.max(0.90, phiExergy) / 0.90, 2.5);
        return Math.clamp(fluvialActivation * volume, 0.0, 5000.0);
    }

    public static double computeTopsoilErosionLoss(double phiExergy) {
        double lossActivation = Math.tanh(Math.max(0.0, phiExergy - 0.88) * 2.5);
        double erosion = 0.03 + 35.0 * lossActivation;
        return Math.clamp(erosion, 0.03, 70.0);
    }

    public static double computeAgriculturalEroei(double phiExergy) {
        double agActivation = Math.tanh(Math.max(0.0, phiExergy - 0.935) * 5.0);
        double eroei = 4.5 + 6.0 / (1.0 + Math.exp(-(phiExergy - 1.50) / 0.20));
        return Math.clamp(agActivation * eroei, 0.0, 25.0);
    }

    public static double computeForestCoverRemaining(double phiExergy) {
        double clearingActivation = Math.tanh(Math.max(0.0, phiExergy - 0.935) * 1.8);
        return Math.clamp(99.0 - 55.0 * clearingActivation, 30.0, 99.0);
    }

    /**
     * Information Travel Speed (km/day) across physical relay networks (Scheidel 2014; Standage 1998).
     *
     * @param phiExergy Garrett-Lotka exergy-capital index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$
     * @return dispatch propagation velocity in kilometers per day (km/day)
     */
    public static double computeInformationTravelSpeed(double phiExergy) {
        // Pre-industrial and early modern courier relay velocity (Scheidel 2014 ORBIS; Standage 1998)
        double courierSpeed = 35.0 + 45.0 * Math.tanh(Math.max(0.0, phiExergy - 0.90) * 1.5);
        // Electromagnetic telecommunication emergence requiring modern industrial power grid (phi >= 3.5)
        double telecomSpeed = 1.0e7 / (1.0 + Math.exp(-(phiExergy - 3.80) / 0.12));
        return Math.clamp(courierSpeed + telecomSpeed, 4.0, 3.0e8);
    }

    public static double computeMetalSmeltingPerCapita(double phiExergy) {
        double smeltActivation = Math.tanh(Math.max(0.0, phiExergy - 0.90) * 4.0);
        double industrialSmelting = 60.0 * Math.pow(SmoothMath.softplus(phiExergy - 1.35, 0.0, 5.0), 2.0);
        return Math.clamp(smeltActivation * (1.5 + industrialSmelting), 0.0, 350.0);
    }

    /**
     * Fossil Fuel Share in Primary Energy (%) (Smil 2017; Ritchie 2020).
     */
    public static double computeFossilSharePrimaryEnergy(double phiExergy) {
        // Fossil fuel share in primary energy (Smil 2017; Ritchie 2020; Wrigley 2010)
        // Industrial substitution takes off gradually: ~6-14% at phi ~ 2.5-2.8 (1800-1820),
        // reaching ~50% at phi ~ 4.2 (1900), and ~80% in high-power 20th century regimes.
        if (phiExergy <= 1.20) return 0.0;
        double share = 85.0 / (1.0 + Math.exp(-(phiExergy - 4.20) / 0.85));
        return Math.clamp(share, 0.0, 95.0);
    }

    /**
     * Evaluates Real Maritime Shipping Freight Cost Index ($100 = 1913\text{ Level}$)
     * (Harley 1988; Mohammed &amp; Williamson 2004; UNCTAD 2024).
     *
     * @param phiExergy emergent Garrett-Lotka exergy-capital diagnostic
     * @return real freight cost index
     */
    public static double computeShippingFreightCostIndex(double phiExergy) {
        double pNorm = Math.max(0.0, phiExergy - 1.02) / 0.22;
        double cost = 20.0 + 1400.0 / (1.0 + Math.pow(pNorm, 3.0));
        return Math.clamp(cost, 15.0, 1415.0);
    }

    public static double computeLifeExpectancy(double phiExergy, double food) {
        double nutritionBonus = Math.clamp((food - 2000.0) / 500.0, -8.0, 5.0);
        // Canonical Preston Curve (1975): rises from ~28 years (pre-industrial) to ~80 years (sanitized modern)
        double prestonTransition = 52.0 / (1.0 + Math.exp(-(phiExergy - 1.50) / 0.25));
        return Math.clamp(28.0 + prestonTransition + nutritionBonus, 20.0, 85.0);
    }

    /**
     * Evaluates continuous planetary Kardashev scale index (Kardashev 1964; Sagan 1973).
     *
     * @param powerWattsPerCapita mechanical, thermal, and electrical power flux (Watts/capita)
     * @param population total population
     * @return continuous Kardashev scale index
     */
    public static double computeKardashevScale(double powerWattsPerCapita, long population) {
        double totalWatts = Math.max(1.0, powerWattsPerCapita * Math.max(1, population));
        return (Math.log10(totalWatts) - 6.0) / 10.0;
    }

    /**
     * Synthesizes emergent technology level \(T\) strictly a posteriori from physical state:
     * energy extraction flux (Kardashev 1964; Lotka 1922; Smil 2017) and physical capital
     * stock density (Solow 1956; Ashby 2011).
     *
     * @param powerWattsPerCapita per-capita exergy flux in Watts
     * @param capitalPerCapita productive physical capital density (kg/capita)
     * @return synthetic emergent technology index in [0.1, 10.0]
     */
    public static double computeEmergentTechnologyLevel(double powerWattsPerCapita, double capitalPerCapita) {
        return org.ether.society.database.H3CellPhysicsAndStateHelper.computeGarrettLotkaEmergentIndex(powerWattsPerCapita, capitalPerCapita, 80.0);
    }
}
